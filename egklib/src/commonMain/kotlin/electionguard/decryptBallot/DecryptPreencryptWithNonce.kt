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
import electionguard.preencrypt.PreEncryptedContest
import electionguard.preencrypt.PreEncryptedSelection
import electionguard.preencrypt.PreEncryptor

/** Decryption of a preencrypted EncryptedBallot using the ballot nonce. */
class DecryptPreencryptWithNonce(
    val group: GroupContext,
    val manifest: Manifest,
    val publicKey: ElGamalPublicKey,
    val extendedBaseHash: UInt256,
    sigma : (UInt256) -> String, // hash trimming function Ω
) {
    private val preEncryptor = PreEncryptor( group, manifest, publicKey.key, extendedBaseHash, sigma)

    fun EncryptedBallot.decrypt(ballotNonce: UInt256): Result<PlaintextBallot, String> {
        require(this.isPreencrypt)

        val preEncryptedBallot = preEncryptor.preencrypt(this.ballotId, this.ballotStyleId, ballotNonce)

        val (plaintextContests, cerrors) = this.contests.map {
            val pcontest = preEncryptedBallot.contests.find { tcontest -> it.contestId == tcontest.contestId}
            if (pcontest == null) {
                Err("Cant find contest ${it.contestId} in manifest")
            } else {
                decryptContest(ballotNonce, it, pcontest)
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

    private fun decryptContest(
        ballotNonce: UInt256,
        contest: EncryptedBallot.Contest,
        pcontest: PreEncryptedContest,
    ): Result<PlaintextBallot.Contest, String> {

        val decryptions: List<PlaintextBallot.Selection> = decryptPreencryption(contest, pcontest)

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

    private fun decryptPreencryption (
        contest: EncryptedBallot.Contest,
        preeContest: PreEncryptedContest,
    ): List<PlaintextBallot.Selection> {
        val nselections = contest.selections.size
        val preEncryption = contest.preEncryption!!

        val combinedNonces = mutableListOf<ElementModQ>()
        repeat(nselections) { idx ->
            val componentNonces = preEncryption.selectedVectors.map { selected ->
                val pv: PreEncryptedSelection = preeContest.selections.find { it.shortCode == selected.shortCode }!!
                pv.selectionNonces[idx]
            }
            val aggNonce: ElementModQ = with(group) { componentNonces.addQ() }
            combinedNonces.add( aggNonce )
        }

        return contest.selections.mapIndexed { idx, selection ->
            val decodedVote = selection.ciphertext.decryptWithNonce(publicKey, combinedNonces[idx])
            PlaintextBallot.Selection(selection.selectionId, selection.sequenceOrder, decodedVote!!)
        }
    }

}