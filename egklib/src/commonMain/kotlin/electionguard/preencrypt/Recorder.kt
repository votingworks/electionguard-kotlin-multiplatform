package electionguard.preencrypt

import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.core.ElGamalPublicKey
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.core.toElementModQ
import electionguard.encrypt.CiphertextBallot
import electionguard.encrypt.Encryptor

/** The crypto part of the "The Recording Tool" */
class Recorder(
    val group: GroupContext,
    val manifest: Manifest,
    jointPublicKey: ElGamalPublicKey,
    extendedBaseHash: UInt256,
) {
    internal val preEncryptor = PreEncryptor( group, manifest, jointPublicKey)
    val encryptor = Encryptor( group, manifest, jointPublicKey, extendedBaseHash)

    internal fun MarkedPreEncryptedBallot.record(
        codeSeed : UInt256,
        primaryNonce: UInt256,
        codeLen: Int,
    ): Pair<RecordedPreBallot, CiphertextBallot> {
        val preInternal = preEncryptor.preencryptInternal(this.ballotId, this.ballotStyleId, primaryNonce)
        val preEncryptedBallot = preInternal.makeExternal()

        // now find the matches
        val mballotContests = this.contests.associateBy { it.contestId }

        val plaintextContests = mutableListOf<PlaintextBallot.Contest>()
        for (pcontest in preEncryptedBallot.contests) {
            val mcontest = mballotContests[pcontest.contestId]
            if (mcontest != null) { // ok to skip contests, encryptedBallot will deal
                plaintextContests.add(mcontest.recordContest(pcontest))
            }
        }

        val recordPreBallot = makeRecordedPreBallot(
            this,
            preInternal.addSelectionCodes(preEncryptedBallot, codeLen),
        )

        val plaintextBallot = PlaintextBallot(
            this.ballotId,
            this.ballotStyleId,
            plaintextContests,
        )

        val ciphertextBallot = encryptor.encryptPre( // TODO
            plaintextBallot,
            codeSeed.toElementModQ(group),
            primaryNonce,
            null,
            preEncryptedBallot.confirmationCode,
        )

        return Pair(recordPreBallot, ciphertextBallot)
    }

    private fun MarkedPreEncryptedContest.recordContest(
        pcontest: PreEncryptedContest,
    ): PlaintextBallot.Contest {

        val plSelections = mutableListOf<PlaintextBallot.Selection>()
        for (selectionCode in this.selectedCodes) { // counting on order
            val plSelection = matchSelection(selectionCode, pcontest) ?:
                throw IllegalArgumentException("Unknown selectionCode ${selectionCode}")
            plSelections.add(PlaintextBallot.Selection(
                plSelection.selectionId,
                plSelection.sequenceOrder,
                1,
            ))
            println("recordContest ${selectionCode} -> vote for selection ${plSelection.selectionId}")
        }

        return PlaintextBallot.Contest(
            pcontest.contestId,
            pcontest.sequenceOrder,
            plSelections,
        )
    }

    fun matchSelection(selectionCode: String, pcontest: PreEncryptedContest): PreEncryptedSelection? {
        // just brute search, could make faster
        return pcontest.selections.find { it.preencryptionHash.cryptoHashString().endsWith(selectionCode) }
    }
}