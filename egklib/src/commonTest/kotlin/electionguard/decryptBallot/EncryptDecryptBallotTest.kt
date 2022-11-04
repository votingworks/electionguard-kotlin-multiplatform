package electionguard.decryptBallot

import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ContestDataStatus
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.Guardian
import electionguard.ballot.Manifest
import electionguard.ballot.makeContestData
import electionguard.ballot.makeDecryptingTrustee
import electionguard.ballot.makeGuardian
import electionguard.core.Base16.toHex
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.core.getSystemTimeInMillis
import electionguard.core.hashElements
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.core.toUInt256
import electionguard.decrypt.DecryptingTrustee
import electionguard.decrypt.Decryptor
import electionguard.encrypt.Encryptor
import electionguard.encrypt.submit
import electionguard.input.RandomBallotProvider
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.publish.Consumer
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Test KeyCeremony Trustee generation and recovered decryption. */
class EncryptDecryptBallotTest {

    @Test
    fun testEncryptDecrypt() {
        val group = productionGroup()
        val configDir = "src/commonTest/data/start"
        val outputDir = "testOut/RecoveredDecryptionTest"
        val trusteeDir = "testOut/RecoveredDecryptionTest/private_data"

        runEncryptDecryptBallot(group, configDir, outputDir, trusteeDir, listOf(1, 2, 3, 4, 5)) // all
        runEncryptDecryptBallot(group, configDir, outputDir, trusteeDir, listOf(2, 3, 4)) // quota
        runEncryptDecryptBallot(group, configDir, outputDir, trusteeDir, listOf(1, 2, 3, 4)) // between
    }
}

private val writeout = false
private val nguardians = 4
private val quorum = 3
private val nballots = 20
private val debug = false

fun runEncryptDecryptBallot(
    group: GroupContext,
    configDir: String,
    outputDir: String,
    trusteeDir: String,
    present: List<Int>,
) {
    val consumerIn = Consumer(configDir, group)
    val config: ElectionConfig = consumerIn.readElectionConfig().getOrThrow { IllegalStateException(it) }

    //// simulate key ceremony
    val trustees: List<KeyCeremonyTrustee> = List(nguardians) {
        val seq = it + 1
        KeyCeremonyTrustee(group, "guardian$seq", seq, quorum)
    }.sortedBy { it.xCoordinate }
    trustees.forEach { t1 ->
        trustees.forEach { t2 ->
            t1.receivePublicKeys(t2.sendPublicKeys().unwrap())
        }
    }
    trustees.forEach { t1 ->
        trustees.forEach { t2 ->
            t2.receiveSecretKeyShare(t1.sendSecretKeyShare(t2.id).unwrap())
        }
    }
    val dTrustees: List<DecryptingTrustee> = trustees.map { makeDecryptingTrustee(it) }
    val guardians: List<Guardian> = trustees.map { makeGuardian(it) }
    val jointPublicKey: ElementModP =
        dTrustees.map { it.electionPublicKey() }.reduce { a, b -> a * b }

    testDecryptor(
        group,
        config.manifest,
        group.TWO_MOD_Q,
        ElGamalPublicKey(jointPublicKey),
        guardians,
        dTrustees,
        present
    )

    //////////////////////////////////////////////////////////
    if (writeout) {
        val commitments: MutableList<ElementModP> = mutableListOf()
        trustees.forEach { commitments.addAll(it.coefficientCommitments()) }
        val commitmentsHash = hashElements(commitments)

        val primes = config.constants
        val cryptoBaseHash: UInt256 = hashElements(
            primes.largePrime.toHex(),
            primes.smallPrime.toHex(),
            primes.generator.toHex(),
            config.numberOfGuardians,
            config.quorum,
            config.manifest.cryptoHash,
        )

        // spec 1.52, eq 17 and 3.B
        val cryptoExtendedBaseHash: UInt256 = hashElements(cryptoBaseHash, jointPublicKey, commitmentsHash)
        val init = ElectionInitialized(
            config,
            jointPublicKey,
            config.manifest.cryptoHash,
            cryptoBaseHash,
            cryptoExtendedBaseHash,
            guardians,
        )
        val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
        publisher.writeElectionInitialized(init)

        val trusteePublisher = Publisher(trusteeDir, PublisherMode.createIfMissing)
        trustees.forEach { trusteePublisher.writeTrustee(trusteeDir, it) }
    }
}

fun testDecryptor(
    group: GroupContext,
    manifest: Manifest,
    qbar: ElementModQ,
    publicKey: ElGamalPublicKey,
    guardians: List<Guardian>,
    trustees: List<DecryptingTrustee>,
    present: List<Int>
) {
    println("present $present")

    val available = trustees.filter { present.contains(it.xCoordinate()) }
    val missing = trustees.filter { !present.contains(it.xCoordinate()) }.map { it.id }

    val encryptor = Encryptor(group, manifest, publicKey, qbar.toUInt256())
    val decryptor = Decryptor(group, qbar, publicKey, guardians, available, missing)

    var encryptTime = 0L
    var decryptTime = 0L
    RandomBallotProvider(manifest, nballots, true).ballots().forEach { ballot ->
        val codeSeed = group.randomElementModQ(minimum = 2)
        val masterNonce = group.randomElementModQ(minimum = 2)

        val startEncrypt = getSystemTimeInMillis()
        val ciphertextBallot = encryptor.encrypt(ballot, codeSeed, masterNonce, 0)
        val encryptedBallot = ciphertextBallot.submit(EncryptedBallot.BallotState.CAST)
        encryptTime += getSystemTimeInMillis() - startEncrypt

        val startDecrypt = getSystemTimeInMillis()
        val decryptedBallot = decryptor.decryptBallot(encryptedBallot)
        decryptTime += getSystemTimeInMillis() - startDecrypt

        // contestData matches
        ballot.contests.forEach { orgContest ->
            val mcontest = manifest.contests.find { it.contestId == orgContest.contestId }!!
            val orgContestData = makeContestData(mcontest.votesAllowed, orgContest.selections, orgContest.writeIns)

            val dcontest = decryptedBallot.contests.values.find { it.contestId == orgContest.contestId }
            assertNotNull(dcontest)
            assertNotNull(dcontest.decryptedContestData)
            assertEquals(dcontest.decryptedContestData!!.contestData.writeIns, orgContestData.writeIns)

            val status = dcontest.decryptedContestData!!.contestData.status
            val overvotes = dcontest.decryptedContestData!!.contestData.overvotes
            if (debug) println(" status = $status overvotes = $overvotes")

            // check if selection votes match
            orgContest.selections.forEach { selection ->
                val dselection = dcontest.selections.values.find { it.selectionId == selection.selectionId }

                if (status == ContestDataStatus.over_vote) {
                    // check if overvote was correctly recorded
                    val hasWriteIn = overvotes.find { it == selection.sequenceOrder } != null
                    assertEquals(selection.vote == 1, hasWriteIn)

                } else {
                    // check if selection votes match
                    assertNotNull(dselection)
                    assertEquals(selection.vote, dselection.tally)
                }
            }
        }
    }

    val encryptPerBallot = (encryptTime.toDouble() / nballots).roundToInt()
    val decryptPerBallot = (decryptTime.toDouble() / nballots).roundToInt()
    println("testDecryptor for $nballots ballots took $encryptPerBallot encrypt, $decryptPerBallot decrypt msecs/ballot")
}