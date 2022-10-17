package electionguard.encrypt

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.unwrapError
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Manifest
import electionguard.core.*
import electionguard.input.RandomBallotProvider
import electionguard.publish.Consumer
import electionguard.verifier.Stats
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Verify a CiphertextBallot. */
class CiphertextBallotVerifyTest {
    val input = "src/commonTest/data/runWorkflowAllAvailable"
    val nballots = 11

    @Test
    fun ciphertextBallotVerifyTest() {
        val group = productionGroup()
        val consumerIn = Consumer(input, group)
        val electionInit: ElectionInitialized =
            consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }

        val encryptor = Encryptor(
            group,
            electionInit.manifest(),
            ElGamalPublicKey(electionInit.jointPublicKey),
            electionInit.cryptoExtendedBaseHash
        )

        val verifier = VerifyCiphertextBallot(
            electionInit.manifest(),
            ElGamalPublicKey(electionInit.jointPublicKey),
            electionInit.cryptoExtendedBaseHash.toElementModQ(group),
        )

        val starting = getSystemTimeInMillis()
        RandomBallotProvider(electionInit.manifest(), nballots).ballots().forEach { ballot ->
            // encrypt
            val codeSeed = group.randomElementModQ(minimum = 2)
            val masterNonce = group.randomElementModQ(minimum = 2)
            val ciphertextBallot = encryptor.encrypt(ballot, codeSeed, masterNonce, 0)

            // verify
            val stats = verifier.verify(ciphertextBallot)
            assertTrue(stats.allOk)

            // decrypt and verify embedded nonces
            val decryptionWithNonce = VerifyEmbeddedNonces(group, electionInit.manifest(), electionInit.jointPublicKey())
            val decryptedBallot = with (decryptionWithNonce) { ciphertextBallot.decrypt() }
            assertNotNull(decryptedBallot)

            // check that votes were correctly decrypted
            compareBallots(ballot, decryptedBallot)
        }

        val took = getSystemTimeInMillis() - starting
        val msecsPerBallot = (took.toDouble() / nballots).roundToInt()
        println("ciphertextBallotVerifyTest $nballots took $took millisecs for $nballots ballots = $msecsPerBallot msecs/ballot")
    }
}

// uses CiphertextBallot instead of EncryptedBallot
private class VerifyCiphertextBallot(
    val manifest: Manifest,
    val jointPublicKey: ElGamalPublicKey,
    val cryptoExtendedBaseHash: ElementModQ) {

    fun verify(ballot: CiphertextBallot): Stats {
        var ncontests = 0
        var nselections = 0
        val errors = mutableListOf<String>()

        val test6 = verifyTrackingCode(ballot)
        if (test6 is Err) {
            errors.add(test6.unwrapError())
        }

        for (contest in ballot.contests) {
            val where = "${ballot.ballotId}/${contest.contestId}"
            ncontests++
            nselections += contest.selections.size

            // calculate ciphertextAccumulation (A, B), note 5.B unneeded because we dont keep (A,B) in election record
            val texts: List<ElGamalCiphertext> = contest.selections.map { it.ciphertext }
            val ciphertextAccumulation: ElGamalCiphertext = texts.encryptedSum()

            // test that the proof is correct; covers 5.C, 5.D, 5.E
            val proof: ConstantChaumPedersenProofKnownNonce = contest.proof
            val cvalid = proof.validate(
                ciphertextAccumulation,
                this.jointPublicKey,
                this.cryptoExtendedBaseHash,
                manifest.contestIdToLimit[contest.contestId]
            )
            if (cvalid is Err) {
                errors.add("    5. ConstantChaumPedersenProofKnownNonce failed for $where = ${cvalid.error} ")
            }

            // TODO I think 5.F and 5.G are not needed because we have simplified proofs.
            //   review when 2.0 verification spec is out

            val svalid = verifySelections(ballot.ballotId, contest)
            if (svalid is Err) {
                errors.add(svalid.error)
            }
        }
        val bvalid = errors.isEmpty()
        return Stats(ballot.ballotId, bvalid, ncontests, nselections, errors)
    }

    // 6.A
    private fun verifyTrackingCode(ballot: CiphertextBallot): Result<Boolean, String> {
        val errors = mutableListOf<Result<Boolean, String>>()

        // LOOK check contest.cryptoHash??
        val cryptoHashCalculated = hashElements(ballot.ballotId, manifest.cryptoHashUInt256(), ballot.contests) // B_i
        if (cryptoHashCalculated != ballot.cryptoHash) {
            errors.add(Err("    6. Test ballot.cryptoHash failed for ${ballot.ballotId} "))
        }

        val trackingCodeCalculated = hashElements(ballot.codeSeed, ballot.timestamp, ballot.cryptoHash)
        if (trackingCodeCalculated != ballot.code) {
            errors.add(Err("    6.A Test ballot.trackingCode failed for ${ballot.ballotId} "))
        }
        return errors.merge()
    }

    private fun verifySelections(ballotId: String, contest: CiphertextBallot.Contest): Result<Boolean, String> {
        val errors = mutableListOf<Result<Boolean, String>>()
        var nplaceholders = 0
        for (selection in contest.selections) {
            val where = "${ballotId}/${contest.contestId}/${selection.selectionId}"
            if (selection.isPlaceholderSelection) nplaceholders++

            // test that the proof is correct covers 4.A, 4.B, 4.C, 4.D
            val svalid = selection.proof.validate(
                selection.ciphertext,
                this.jointPublicKey,
                this.cryptoExtendedBaseHash,
            )
            if (svalid is Err) {
                errors.add(Err("    4. DisjunctiveChaumPedersenProofKnownNonce failed for $where/${selection.selectionId} = ${svalid.error} "))
            }

            // TODO I think 4.E, 4.F, 4.G, 4.H are not needed because we have simplified proofs.
            //   review when 2.0 verification spec is out
        }

        // 5.A verify the placeholder numbers match the maximum votes allowed
        val limit = manifest.contestIdToLimit[contest.contestId]
        if (limit == null) {
            errors.add(Err(" 5. Contest ${contest.contestId} not in Manifest"))
        } else {
            if (limit != nplaceholders) {
                errors.add(Err(" 5.A Contest placeholder $nplaceholders != $limit vote limit for contest ${contest.contestId}"))
            }
        }
        return errors.merge()
    }
}