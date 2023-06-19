package electionguard.decryptBallot

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.partition
import com.github.michaelbull.result.unwrap
import electionguard.ballot.*
import electionguard.core.*

/** Decryption of an EncryptedBallot using the ballot nonce. */
class DecryptWithNonce(val group : GroupContext, val manifest: ManifestIF, val publicKey: ElGamalPublicKey,
                       val extendedBaseHash: UInt256) {

    fun EncryptedBallot.decrypt(ballotNonce: UInt256): Result<PlaintextBallot, String> {
        require(!this.isPreencrypt)

        val (plaintextContests, cerrors) = this.contests.map {
            // TODO only use of manifest is to check contest existance
            val mcontest = manifest.contests.find { tcontest -> it.contestId == tcontest.contestId}
            if (mcontest == null) {
                Err("Cant find contest ${it.contestId} in manifest")
            } else {
                decryptContestWithPrimaryNonce(ballotNonce, it)
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
        ballotNonce: UInt256,
        contest: EncryptedBallot.Contest,
    ): Result<PlaintextBallot.Contest, String> {

        val decryptions = mutableListOf<PlaintextBallot.Selection>()
        val errors = mutableListOf<String>()
        for (selection in contest.selections) {
            val dSelection = decryptSelectionWithPrimaryNonce(ballotNonce, contest.contestId, selection)
            if (dSelection == null) {
                errors.add(" decryption with nonce failed for contest: '${contest.contestId}' selection: '${selection.selectionId}'")
            } else {
                decryptions.add(dSelection)
            }
        }
        if (errors.isNotEmpty()) {
            return Err(errors.joinToString("\n"))
        }

        // contest data
        val contestDataResult = contest.contestData.decryptWithNonceToContestData(
            publicKey,
            extendedBaseHash,
            contest.contestId,
            ballotNonce)

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

}