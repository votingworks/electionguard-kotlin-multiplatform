package electionguard.decrypt

import electionguard.ballot.EncryptedBallot
import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.Nonces
import electionguard.core.UInt256
import electionguard.core.decryptWithNonce
import electionguard.core.get
import electionguard.core.hashElements
import electionguard.core.toElementModQ
import mu.KotlinLogging

private val logger = KotlinLogging.logger("DecryptionWithMasterNonce")

/** Decryption of a EncryptedBallot using the master nonce. */
class DecryptionWithMasterNonce(val group : GroupContext, val publicKey: ElGamalPublicKey) {

    fun decrypt(manifest: Manifest, masterNonce: ElementModQ, ballot: EncryptedBallot): PlaintextBallot? {
        val ballotNonce: UInt256 = hashElements(manifest.cryptoHash, ballot.ballotId, masterNonce)

        val plaintext_contests = mutableListOf<PlaintextBallot.Contest>()
        for (contest in ballot.contests) {
            val mcontest = manifest.contests.find { it.contestId == contest.contestId}
            if (mcontest == null) {
                logger.warn { "Cant find contest ${contest.contestId} in manifest"}
                return null
            }
            val plaintextContest = decryptContestWithMasterNonce(mcontest, ballotNonce, contest)
            if (plaintextContest == null) {
                return null
            }
            plaintext_contests.add(plaintextContest)
        }
        return PlaintextBallot(
            ballot.ballotId,
            ballot.ballotStyleId,
            plaintext_contests,
            null
        )
    }

    private fun decryptContestWithMasterNonce(
        mcontest: Manifest.ContestDescription,
        ballotNonce: UInt256,
        contest: EncryptedBallot.Contest
    ): PlaintextBallot.Contest? {
        val contestDescriptionHash = mcontest.cryptoHash
        val contestDescriptionHashQ = contestDescriptionHash.toElementModQ(group)
        val nonceSequence = Nonces(contestDescriptionHashQ, ballotNonce)
        val contestNonce = nonceSequence[contest.sequenceOrder]

        val plaintextSelections = mutableListOf<PlaintextBallot.Selection>()
        for (selection in contest.selections.filter { !it.isPlaceholderSelection }) {
            val mselection = mcontest.selections.find { it.selectionId == selection.selectionId }
            if (mselection == null) {
                logger.warn { "Cant find selection ${selection.selectionId} in contest ${mcontest.contestId}" }
                return null
            }
            val plaintextSelection = decryptSelectionWithMasterNonce(mselection, contestNonce, selection)
            if (plaintextSelection == null) {
                logger.warn { "Selection ${selection.selectionId} in contest ${mcontest.contestId} failed to decrypt" }
                return null
            }
            plaintextSelections.add(plaintextSelection)
        }
        return PlaintextBallot.Contest(
            contest.contestId,
            contest.sequenceOrder,
            plaintextSelections
        )
    }

    private fun decryptSelectionWithMasterNonce(
        mselection: Manifest.SelectionDescription,
        contestNonce: ElementModQ,
        selection: EncryptedBallot.Selection
    ): PlaintextBallot.Selection? {
        val nonceSequence = Nonces(mselection.cryptoHash.toElementModQ(group), contestNonce)
        val selectionNonce: ElementModQ = nonceSequence[selection.sequenceOrder]

        val decodedVote: Int? = selection.ciphertext.decryptWithNonce(publicKey, selectionNonce)
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