package electionguard.preencrypt

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Manifest
import electionguard.core.ElGamalPublicKey
import electionguard.core.UInt256
import electionguard.core.hashElements
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.core.runTest
import electionguard.core.toElementModQ
import electionguard.core.toUInt256
import electionguard.encrypt.cast
import electionguard.input.ManifestInputBuilder
import electionguard.protoconvert.importEncryptedBallot
import electionguard.protoconvert.publishEncryptedBallot
import electionguard.publish.Consumer
import electionguard.verifier.VerifyEncryptedBallots
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

private val random = Random
private const val codeLen = 4

internal class PreEncryptorTest {
    val input = "src/commonTest/data/runWorkflowAllAvailable"

    // sanity check that PreEncryptor.preencrypt doesnt barf
    @Test
    fun testPreencrypt() {
        runTest {
            val group = productionGroup()
            val consumerIn = Consumer(input, group)
            val electionInit: ElectionInitialized =
                consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }
            val manifest = electionInit.manifest()

            val preEncryptor =
                PreEncryptor(group, manifest, electionInit.jointPublicKey())

            manifest.ballotStyles.forEach { println(it) }

            val pballot = preEncryptor.preencrypt("testPreencrypt_ballot_id", "styling", 11U.toUInt256())
            pballot.show()
        }
    }

    // sanity check that Recorder.record doesnt barf
    @Test
    fun testRecord() {
        runTest {
            val group = productionGroup()
            val consumerIn = Consumer(input, group)
            val electionInit: ElectionInitialized =
                consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }
            val manifest = electionInit.manifest()

            val preEncryptor =
                PreEncryptor(group, manifest, electionInit.jointPublicKey())

            manifest.ballotStyles.forEach { println(it) }

            val primaryNonce = 42U.toUInt256()
            val pballot = preEncryptor.preencrypt("testDecrypt_ballot_id", "styling", primaryNonce)
            pballot.show()

            val mballot = markBallotChooseOne(manifest, pballot)
            mballot.show()

            val recorder = Recorder(group, manifest, electionInit.jointPublicKey(), electionInit.cryptoExtendedBaseHash)

            val ciphertextBallot = with(recorder) {
                mballot.record(UInt256.TWO, primaryNonce, codeLen)
            }
        }
    }

    // check that CiphertextBallot is correctly formed
    @Test
    fun testSingleLimit() {
        runTest {
            val ebuilder = ManifestInputBuilder("testSingleLimit")
            val manifest: Manifest = ebuilder.addContest("onlyContest")
                .addSelection("selection1", "candidate1")
                .addSelection("selection2", "candidate2")
                .done()
                .build()

            runComplete("testSingleLimit", manifest, this::markBallotChooseOne, true)
        }
    }

    // multiple selections per contest
    @Test
    fun testMultipleSelections() {
        runTest {
            val ebuilder = ManifestInputBuilder("testMultipleSelections")
            val manifest: Manifest = ebuilder.addContest("onlyContest")
                .setVoteVariationType(Manifest.VoteVariationType.n_of_m, 2)
                .addSelection("selection1", "candidate1")
                .addSelection("selection2", "candidate2")
                .addSelection("selection3", "candidate3")
                .done()
                .build()

            runComplete("testMultipleSelections", manifest, this::markBallotToLimit, false)
        }
    }

    fun runComplete(
        ballot_id: String,
        manifest: Manifest,
        markBallot: (manifest: Manifest, pballot: PreEncryptedBallot) -> MarkedPreEncryptedBallot,
        isLimitOne: Boolean,
    ) {
        val group = productionGroup()

        val qbar = 4242U.toUInt256()
        val secret = group.randomElementModQ(minimum = 1)
        val publicKey = ElGamalPublicKey(group.gPowP(secret))

        // pre-encrypt
        val preEncryptor =
            PreEncryptor(group, manifest, publicKey)
        val primaryNonce = 42U.toUInt256()
        val pballot = preEncryptor.preencrypt(ballot_id, "styling", primaryNonce)

        // vote
        val mballot = markBallot(manifest, pballot)
        mballot.show()

        // record
        val recorder = Recorder(group, manifest, publicKey, qbar)
        val (recordedBallot, ciphertextBallot) = with(recorder) {
            mballot.record(UInt256.TWO, primaryNonce, codeLen)
        }

        val mcontests = mballot.contests.associateBy { it.contestId }
        println("\nCiphertextBallot ${ciphertextBallot.ballotId}")
        for (contest in ciphertextBallot.contests) {
            println(" contest ${contest.contestId}")
            if (isLimitOne) {
                val ve = contest.selections.filter { !it.isPlaceholderSelection }.map { it.ciphertext }
                val hv = hashElements(ve)
                contest.selections.forEach { println("   ${it.selectionId} = ${it.ciphertext.cryptoHashUInt256().cryptoHashString()}")}

                val mcontest =
                    mcontests[contest.contestId] ?: throw IllegalArgumentException("Unknown contest $contest.contestId")
                mcontest.selectedCodes.forEach {
                    println("  hv = ${hv.cryptoHashString()} endsWith $it")
                    assertTrue(hv.cryptoHashString().endsWith(it))
                }
            }
        }

        recordedBallot.show()
        val encryptedBallot = ciphertextBallot.cast()

        // roundtrip
        val proto = encryptedBallot.publishEncryptedBallot(recordedBallot)
        // combines the recordedBallot
        val fullEncryptedBallot = group.importEncryptedBallot(proto).unwrap()

        val verifier = VerifyEncryptedBallots(group, manifest, publicKey, qbar.toElementModQ(group), 1)
        val stats = verifier.verifyEncryptedBallot(fullEncryptedBallot)
        println("VerifyEncryptedBallots $stats")
        assertTrue(stats.result is Ok)
    }

    fun markBallotChooseOne(manifest: Manifest, pballot: PreEncryptedBallot): MarkedPreEncryptedBallot {
        val pcontests = mutableListOf<MarkedPreEncryptedContest>()
        for (pcontest in pballot.contests) {
            val n = pcontest.selections.size
            val idx = random.nextInt(n)
            val pselection = pcontest.selections[idx]
            pcontests.add(
                MarkedPreEncryptedContest(
                    pcontest.contestId,
                    listOf(pselection.choose(codeLen)),
                )
            )
        }

        return MarkedPreEncryptedBallot(
            pballot.ballotId,
            pballot.ballotStyleId,
            pcontests,
        )
    }

    // pick all selections 0..limit-1
    fun markBallotToLimit(manifest: Manifest, pballot: PreEncryptedBallot): MarkedPreEncryptedBallot {
        val pcontests = mutableListOf<MarkedPreEncryptedContest>()
        for (pcontest in pballot.contests) {
            val mcontest = manifest.contests.find { it.contestId == pcontest.contestId }
                ?: throw IllegalArgumentException("Cant find $pcontest.contestId")
            val selections = mutableListOf<String>()
            for (idx in 0 until mcontest.votesAllowed) {
                selections.add(pcontest.selections[idx].choose(codeLen))
            }
            pcontests.add(
                MarkedPreEncryptedContest(
                    pcontest.contestId,
                    selections,
                )
            )
        }

        return MarkedPreEncryptedBallot(
            pballot.ballotId,
            pballot.ballotStyleId,
            pcontests,
        )
    }

    fun PreEncryptedSelection.choose(len: Int): String {
        val match = this.preencryptionHash.cryptoHashString()
        return match.substring(match.length - len) // arbitrary, for testing purposes
    }
}