package electionguard.decryptBallot

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.partition
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ContestDataStatus
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.decryptWithNonceToContestData
import electionguard.core.*

/** Decryption of a EncryptedBallot using the ballot nonce. */
class DecryptionWithPrimaryNonce(val group : GroupContext, val manifest: Manifest, val publicKey: ElGamalPublicKey,
    val extendedBaseHash: UInt256) {

    fun EncryptedBallot.decrypt(ballotNonce: UInt256): Result<PlaintextBallot, String> {

        val (plaintextContests, cerrors) = this.contests.map {
            val mcontest = manifest.contests.find { tcontest -> it.contestId == tcontest.contestId}
            if (mcontest == null) {
                Err("Cant find contest ${it.contestId} in manifest")
            } else {
                decryptContestWithPrimaryNonce(this.isPreencrypt, ballotNonce, it)
            }
        }.partition()

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

    private fun decryptContestWithPrimaryNonce(
        isPreencrypt : Boolean,
        ballotNonce: UInt256,
        contest: EncryptedBallot.Contest
    ): Result<PlaintextBallot.Contest, String> {

        val decryptions: List<PlaintextBallot.Selection> = if (isPreencrypt) {
            decryptPreencryption(ballotNonce, contest)
        } else {
            val dSelections = mutableListOf<PlaintextBallot.Selection>()
            val errors = mutableListOf<String>()
            for (selection in contest.selections) {
                val dSelection = decryptSelectionWithPrimaryNonce(ballotNonce, contest.contestId, selection)
                if (dSelection == null) {
                    errors.add(" decryption with nonce failed for contest: '${contest.contestId}' selection: '${selection.selectionId}'")
                } else {
                    dSelections.add(dSelection)
                }
            }
            if (errors.isNotEmpty()) {
                return Err(errors.joinToString("\n"))
            }
            dSelections
        }

        // contest data
        val contestDataNonce = hashFunction(extendedBaseHash.bytes, 0x20.toByte(), ballotNonce, contest.contestId, "contest data")
        val contestDataResult = contest.contestData.decryptWithNonceToContestData(publicKey, contestDataNonce.toElementModQ(group))
        if (contestDataResult is Err) {
            return contestDataResult
        }
        val contestData = contestDataResult.unwrap()

        // on overvote, modify selections to use original votes
        val useSelections = if (contestData.status == ContestDataStatus.over_vote) {
            // set the selections to the original
            decryptions.map { dselection ->
                if (contestData.overvotes.find { it == dselection.sequenceOrder } == null) dselection
                else dselection.copy(vote = 1)
            }
        } else {
            decryptions
        }

        return Ok(PlaintextBallot.Contest(
            contest.contestId,
            contest.sequenceOrder,
            useSelections,
            contestData.writeIns,
        ))
    }

    private fun decryptSelectionWithPrimaryNonce(
        ballotNonce: UInt256,
        contestLabel: String,
        selection: EncryptedBallot.Selection
    ): PlaintextBallot.Selection? {

        val selectionNonce = hashFunction(extendedBaseHash.bytes, 0x20.toByte(), ballotNonce, contestLabel, selection.selectionId) // eq 22
        val decodedVote: Int? = selection.ciphertext.decryptWithNonce(publicKey, selectionNonce.toElementModQ(group))

        return decodedVote?.let {
            PlaintextBallot.Selection(
                selection.selectionId,
                selection.sequenceOrder,
                decodedVote,
            )
        }
    }

    private fun decryptPreencryption (
        ballotNonce: UInt256,
        contest: EncryptedBallot.Contest
    ): List<PlaintextBallot.Selection> {
        val contestLabel = contest.contestId
        val nselections = contest.selections.size

        // find out which of the selections generated the nonces
        var genSelection : String? = null
        for (selectioni in contest.selections) {
            var allOk = true
            for (selectionj in contest.selections) {
                val selectionNonce = hashFunction(extendedBaseHash.bytes, 0x43.toByte(), ballotNonce, contestLabel, selectioni.selectionId, selectionj.selectionId) // eq 97
                if (null == selectionj.ciphertext.decryptWithNonce(publicKey, selectionNonce.toElementModQ(group), nselections)) {
                    allOk = false
                    break
                }
            }
            // if we get to here, then all the encryptions worked, so selectioni is the one we want
            if (allOk) {
                genSelection = selectioni.selectionId
                break
            }
        }
        if ( genSelection == null) {
            genSelection = "null1"
        }

        return contest.selections.map { selection ->
            val selectionNonce = hashFunction(extendedBaseHash.bytes, 0x43.toByte(), ballotNonce, contestLabel, genSelection, selection.selectionId)
            val decodedVote = selection.ciphertext.decryptWithNonce(publicKey, selectionNonce.toElementModQ(group))
            PlaintextBallot.Selection(selection.selectionId, selection.sequenceOrder, decodedVote!!)
        }
    }
}