package electionguard.decrypt

import electionguard.ballot.PlaintextBallot
import electionguard.core.ElGamalPublicKey
import electionguard.core.decryptWithNonce
import electionguard.encrypt.CiphertextBallot
import mu.KotlinLogging

private val logger = KotlinLogging.logger("DecryptionWithEmbeddedNonces")

/** Decryption of a CiphertextBallot using the embedded nonces. */
class DecryptionWithEmbeddedNonces(val publicKey: ElGamalPublicKey) {

    fun decrypt(ballot: CiphertextBallot): PlaintextBallot? {
        val plaintextContests = mutableListOf<PlaintextBallot.Contest>()
        for (contest in ballot.contests) {
            val plaintextContest = decryptContestWithNonces(contest)
            if (plaintextContest == null) {
                logger.warn { "decryption with nonce failed for ballot: ${ballot.ballotId} contest ${contest.contestId}" }
                return null
            } else {
                plaintextContests.add(plaintextContest)
            }
        }
        return PlaintextBallot(
            ballot.ballotId,
            ballot.ballotStyleId,
            plaintextContests,
            null
        )
    }

    private fun decryptContestWithNonces(contest: CiphertextBallot.Contest): PlaintextBallot.Contest? {
        val plaintextSelections = mutableListOf<PlaintextBallot.Selection>()
        for (selection in contest.selections.filter {!it.isPlaceholderSelection}) {
            val plaintextSelection = decryptSelectionWithNonces(selection)
            if (plaintextSelection == null) {
                logger.warn { "decryption with nonce failed for contest: ${contest.contestId} selection: ${selection.selectionId}" }
                return null
            } else {
                plaintextSelections.add(plaintextSelection)
            }
        }
        return PlaintextBallot.Contest(
                contest.contestId,
                contest.sequenceOrder,
                plaintextSelections
            )
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