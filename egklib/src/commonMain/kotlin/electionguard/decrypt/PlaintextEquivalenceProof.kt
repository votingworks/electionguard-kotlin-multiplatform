package electionguard.decrypt

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.EncryptedBallot
import electionguard.core.*
import electionguard.input.ValidationMessages
import mu.KotlinLogging

private val logger = KotlinLogging.logger("DistPep")

// "Distributed Plaintext Equivalence Proof"
class PlaintextEquivalenceProof(
    val group: GroupContext,
    val extendedBaseHash: UInt256,
    val jointPublicKey: ElGamalPublicKey,
    val guardians: Guardians, // all guardians
    decryptingTrustees: List<DecryptingTrusteeIF>, // the trustees available to decrypt
) {
    // val offset : ElGamalCiphertext = 10.encrypt(jointPublicKey) // add 10 to keep the ratio positive

    val decryptor = DecryptorDoerre(group, extendedBaseHash, jointPublicKey, guardians, decryptingTrustees)

    // create proof that ballot1 and ballot2 are equivilent
    fun makeProof(ballot1: EncryptedBallot, ballot2: EncryptedBallot): Result<DecryptedTallyOrBallot, String> {
        // LOOK check ballotIds match, styleIds?
        val ballotMesses = ValidationMessages("Ballot '${ballot1.ballotId}'", 1)
        val ratioBallot = makeRatioBallot(ballot1, ballot2, ballotMesses)

        if (ballotMesses.hasErrors()) {
            val message = "${ballot1.ballotId} makeProof did not validate because $ballotMesses"
            logger.atWarn().log(message)
            return Err(message)
        }

        // now run that through the usual decryption
        try {
            val decryption = decryptor.decryptPep(ratioBallot)
            return Ok(decryption)
        } catch (t : DLogException) {
            return Err(t.message?: "no message")
        }
    }

    // make a ballot replacing all the selection ciphertexts with the ratio of ballot1/ballot2 selection ciphertexts
    // also make sure that the two ballots have identical contests and selections
    fun makeRatioBallot(
        ballot1: EncryptedBallot,
        ballot2: EncryptedBallot,
        ballotMesses: ValidationMessages
    ): EncryptedBallot {
        val ratioContests = mutableListOf<EncryptedBallot.Contest>()
        // also make sure that the two ballots have identical contests and selections
        val contest1Ids = ballot1.contests.associateBy { it.contestId }
        for (contest2 in ballot2.contests) {
            if (!contest1Ids.contains(contest2.contestId)) {
                ballotMesses.add("ballot1 missing contest id '${contest2.contestId}'")
            } else {
                ratioContests.add(makeRatioContest(contest1Ids[contest2.contestId]!!, contest2, ballotMesses))
            }
        }
        val contest2Ids = ballot2.contests.associateBy { it.contestId }
        for (contest1 in ballot1.contests) {
            if (!contest2Ids.contains(contest1.contestId)) {
                ballotMesses.add("ballot2 missing contest id '${contest1.contestId}'")
            }
        }
        return ballot1.copy(contests = ratioContests)
    }

    private fun makeRatioContest(
        contest1: EncryptedBallot.Contest,
        contest2: EncryptedBallot.Contest,
        ballotMesses: ValidationMessages
    ): EncryptedBallot.Contest {
        val contestMesses = ballotMesses.nested("Contest " + contest1.contestId)

        if (contest1.sequenceOrder != contest2.sequenceOrder) {
            val msg = "ballot1 contest '${contest1.contestId}' sequenceOrder ${contest1.sequenceOrder} " +
                    " does not match manifest Ballot2 contest sequenceOrder ${contest2.sequenceOrder}"
            contestMesses.add(msg)
        }

        val ratioSelections = mutableListOf<EncryptedBallot.Selection>()
        val selection1Ids = contest1.selections.associateBy { it.selectionId }
        for (selection2 in contest2.selections) {
            if (!selection1Ids.contains(selection2.selectionId)) {
                ballotMesses.add("contest1 missing selection id '${selection2.selectionId}'")
            } else {
                ratioSelections.add(
                    makeRatioSelection(
                        selection1Ids[selection2.selectionId]!!,
                        selection2,
                        contestMesses
                    )
                )
            }
        }

        val selection2Ids = contest2.selections.associateBy { it.selectionId }
        for (selection1 in contest1.selections) {
            if (!selection2Ids.contains(selection1.selectionId)) {
                ballotMesses.add("contest2 missing selection id '${selection1.selectionId}'")
            }
        }
        // make a copy, replacing the selections. other fields no longer valid.
        return contest1.copy(selections = ratioSelections)
    }

    private fun makeRatioSelection(
        selection1: EncryptedBallot.Selection,
        selection2: EncryptedBallot.Selection,
        contestMesses: ValidationMessages
    ): EncryptedBallot.Selection {
        val selectionMesses = contestMesses.nested("Selection " + selection1.selectionId)

        if (selection1.sequenceOrder != selection2.sequenceOrder) {
            val msg = "ballot1 selection '${selection1.selectionId}' sequenceOrder ${selection1.sequenceOrder} " +
                    " does not match ballot2 selection sequenceOrder ${selection2.sequenceOrder}"
            selectionMesses.add(msg)
        }

        val ciphertext1 = selection1.encryptedVote
        // val ciphertext1WithOffset = selection1.encryptedVote.plus(offset)
        val ciphertext2 = selection2.encryptedVote
        val ratio = ElGamalCiphertext(ciphertext1.pad div ciphertext2.pad, ciphertext1.data div ciphertext2.data)
        // make a copy, replacing the ciphertext with the ratio. proofs no longer valid.
        return selection1.copy(encryptedVote = ratio)
    }
}