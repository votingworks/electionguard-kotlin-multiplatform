package electionguard.preencrypt

import electionguard.ballot.Manifest
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.Nonces
import electionguard.core.UInt256
import electionguard.core.encrypt
import electionguard.core.get
import electionguard.core.hashElements
import electionguard.core.toElementModQ

/** The crypto part of the "The Encrypting Tool" */
class PreEncryptor(
    val group: GroupContext,
    val manifest: Manifest,
    val nonceEncryptionKey: ElGamalPublicKey,
) {
    fun preencrypt(
        ballotId: String,
        ballotStyleId: String, // matches one in the manifest
        primaryNonce: UInt256,
    ): PreEncryptedBallot {
        return preencryptInternal(ballotId, ballotStyleId, primaryNonce).makeExternal()
    }

    internal fun preencryptInternal(
        ballotId: String,
        ballotStyleId: String,
        primaryNonce: UInt256,
    ): PreBallotInternal {
        val ballotNonce: UInt256 = hashElements(UInt256.ONE, ballotId, primaryNonce)
        val mcontests = manifest.styleToContestsMap[ballotStyleId]
            ?: throw IllegalArgumentException("Unknown ballotStyleId $ballotStyleId")

        val contestsSorted = mcontests.sortedBy { it.sequenceOrder }
        val contestsInternal = contestsSorted.map { it.preencryptContest(ballotNonce) }

        return PreBallotInternal(
            ballotId,
            ballotStyleId,
            contestsInternal,
        )
    }

    private fun Manifest.ContestDescription.preencryptContest(
        ballotNonce: UInt256,
    ): PreContestInternal {
        val contestNonce= Nonces(this.contestHash.toElementModQ(group), ballotNonce)[0]

        // for each selection
        val selectionsSorted = this.selections.sortedWith(compareBy { it.sequenceOrder })
        val selectionsInternal = selectionsSorted.map {
            it.preencryptSelection(contestNonce)
        }

        return PreContestInternal(
            this.contestId,
            this.sequenceOrder,
            this.contestHash,
            selectionsInternal,
            this.votesAllowed,
        )
    }

    private fun Manifest.SelectionDescription.preencryptSelection(
        contestNonce: ElementModQ,
    ): PreSelectionInternal {
        val selectionNonce = Nonces(this.selectionHash.toElementModQ(group), contestNonce)[1]

        return PreSelectionInternal(
            this.selectionId,
            this.sequenceOrder,
            this.selectionHash,
            0.encrypt(nonceEncryptionKey, selectionNonce),
            1.encrypt(nonceEncryptionKey, selectionNonce),
        )
    }
}