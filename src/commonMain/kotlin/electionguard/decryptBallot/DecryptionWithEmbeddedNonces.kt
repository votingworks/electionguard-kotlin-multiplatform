package electionguard.decryptBallot

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.partition
import electionguard.ballot.PlaintextBallot
import electionguard.core.ElGamalPublicKey
import electionguard.core.decryptWithNonce
import electionguard.encrypt.CiphertextBallot

/** Decryption of a CiphertextBallot using the embedded nonces. */
class DecryptionWithEmbeddedNonces(val publicKey: ElGamalPublicKey) {

    fun CiphertextBallot.decrypt(): Result<PlaintextBallot, String> {
        val (plaintextContests, cerrors) = this.contests.map { decryptContestWithNonces(it) }.partition()
        if (cerrors.isNotEmpty()) {
            return Err(cerrors.joinToString("\n"))
        }
        return Ok(PlaintextBallot(
            this.ballotId,
            this.ballotStyleId,
            plaintextContests,
            null
        ))
    }

    private fun decryptContestWithNonces(contest: CiphertextBallot.Contest): Result<PlaintextBallot.Contest, String> {
        val plaintextSelections = mutableListOf<PlaintextBallot.Selection>()
        val errors = mutableListOf<String>()
        for (selection in contest.selections.filter {!it.isPlaceholderSelection}) {
            val plaintextSelection = decryptSelectionWithNonces(selection)
            if (plaintextSelection == null) {
                errors.add(" decryption with nonces failed for contest: ${contest.contestId} selection: ${selection.selectionId}")
            } else {
                plaintextSelections.add(plaintextSelection)
            }
        }
        if (errors.isNotEmpty()) {
            return Err(errors.joinToString("\n"))
        }
        return Ok(PlaintextBallot.Contest(
            contest.contestId,
            contest.sequenceOrder,
            plaintextSelections
        ))
    }

    private fun decryptSelectionWithNonces(selection: CiphertextBallot.Selection): PlaintextBallot.Selection? {
        val decodedVote: Int? = selection.ciphertext.decryptWithNonce(publicKey, selection.selectionNonce)
        return decodedVote?.let {
            PlaintextBallot.Selection(
                selection.selectionId,
                selection.sequenceOrder,
                decodedVote,
                null
            )
        }
    }
}