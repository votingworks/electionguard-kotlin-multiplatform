package electionguard.decrypt

import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Guardian
import electionguard.ballot.makeDecryptingTrustee
import electionguard.ballot.makeGuardian
import electionguard.core.Base16.toHex
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.core.encrypt
import electionguard.core.hashElements
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.publish.makeConsumer
import electionguard.publish.makePublisher
import kotlin.test.Test
import kotlin.test.assertEquals

/** Test KeyCeremony Trustee generation and recovered decryption. */
class EncryptDecryptTest {
    @Test
    fun testEncryptDecrypt() {
        val group = productionGroup()
        val configDir = "src/commonTest/data/start"
        val outputDir = "testOut/RecoveredDecryptionTest"
        val trusteeDir = "testOut/RecoveredDecryptionTest/private_data"

        runRecoveredDecryption52(group, configDir, outputDir, trusteeDir, listOf(1,2,3,4,5)) // all
        runRecoveredDecryption52(group, configDir, outputDir, trusteeDir, listOf(2,3,4)) // quota
        runRecoveredDecryption52(group, configDir, outputDir, trusteeDir, listOf(1,2,3,4)) // between
    }
}

private val writeout = false
private val nguardians = 4
private val quorum = 3

fun runRecoveredDecryption52(
    group: GroupContext,
    configDir: String,
    outputDir: String,
    trusteeDir: String,
    present: List<Int>,
) {
    val trustees: List<KeyCeremonyTrustee> = List(nguardians) {
        val seq = it + 1
        KeyCeremonyTrustee(group, "guardian$seq", seq, quorum)
    }.sortedBy { it.xCoordinate }

    // exchange PublicKeys
    trustees.forEach { t1 ->
        trustees.forEach { t2 ->
            t1.receivePublicKeys(t2.publicKeys().unwrap())
        }
    }

    // exchange SecretKeyShares
    trustees.forEach { t1 ->
        trustees.filter { it.id != t1.id }.forEach { t2 ->
            t2.receiveEncryptedKeyShare(t1.encryptedKeyShareFor(t2.id).unwrap())
        }
    }

    val dTrustees: List<DecryptingTrustee> = trustees.map { makeDecryptingTrustee(it) }

    val jointPublicKey: ElementModP =
        dTrustees.map { it.electionPublicKey() }.reduce { a, b -> a * b }

    testEncryptRecoveredDecrypt(group, ElGamalPublicKey(jointPublicKey), dTrustees, present)

    //////////////////////////////////////////////////////////
    if (writeout) {
        val commitments: MutableList<ElementModP> = mutableListOf()
        trustees.forEach { commitments.addAll(it.coefficientCommitments()) }
        val commitmentsHash = hashElements(commitments)

        val consumerIn = makeConsumer(configDir, group)
        val config: ElectionConfig = consumerIn.readElectionConfig().getOrThrow { IllegalStateException(it) }

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
        val guardians: List<Guardian> = trustees.map { makeGuardian(it) }
        val init = ElectionInitialized(
            config,
            jointPublicKey,
            config.manifest.cryptoHash,
            cryptoBaseHash,
            cryptoExtendedBaseHash,
            guardians,
        )
        val publisher = makePublisher(outputDir)
        publisher.writeElectionInitialized(init)

        val trusteePublisher = makePublisher(trusteeDir)
        trustees.forEach { trusteePublisher.writeTrustee(trusteeDir, it) }
    }
}

fun testEncryptRecoveredDecrypt(group: GroupContext,
                                publicKey: ElGamalPublicKey,
                                trustees: List<DecryptingTrustee>,
                                present: List<Int>) {
    println("present $present")
    val vote = 42
    val evote = vote.encrypt(publicKey, group.randomElementModQ(minimum = 1))

    val available = trustees.filter {present.contains(it.xCoordinate())}
    val missing = trustees.filter {!present.contains(it.xCoordinate())}.map { it.id }
    val coordsPresent = available.map {it.xCoordinate}
    // once the set of available guardians is determined, the lagrangeCoefficients can be calculated for all decryptions
    val lagrangeCoefficients = available.associate { it.id to group.computeLagrangeCoefficient(it.xCoordinate, coordsPresent) }

    // configure the DecryptingTrustees
    for (decryptingTrustee in available) {
        val lc = lagrangeCoefficients[decryptingTrustee.id()]
            ?: throw RuntimeException("missing available $decryptingTrustee.id()")
        decryptingTrustee.setMissing(group, lc, missing)
    }

    val shares: List<PartialDecryption> = available.map {
        it.decrypt(
            group,
            listOf(evote.pad),
        )[0]
    }

    val Mbar: ElementModP = with(group) { shares.map { it.mbari}.multP() }
    val decryptedValue: ElementModP = evote.data / Mbar
    val dlogM: Int = publicKey.dLog(decryptedValue, 100) ?: throw RuntimeException("dlog failed")
    println("The answer is $dlogM")
    assertEquals(42, dlogM)
}



