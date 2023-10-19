package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.partition
import com.github.michaelbull.result.toResultOr
import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedTally
import electionguard.core.GroupContext

fun electionguard.protogen.EncryptedTally.import(group: GroupContext): Result<EncryptedTally, String> {
    val (contests, errors) = this.contests.map { it.import(group) }.partition()
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }
    return Ok(EncryptedTally(this.tallyId, contests, this.castBallotIds))
}

private fun electionguard.protogen.EncryptedTallyContest.import(group: GroupContext):
        Result<EncryptedTally.Contest, String> {

    val (selections, serrors) = this.selections.map { it.import(group) }.partition()

    if (serrors.isNotEmpty()) {
        return Err(serrors.joinToString("\n"))
    }
    return Ok(EncryptedTally.Contest(this.contestId, this.sequenceOrder, selections))
}

private fun electionguard.protogen.EncryptedTallySelection.import(group: GroupContext):
        Result<EncryptedTally.Selection, String> {

    val ciphertext = group.importCiphertext(this.encryptedVote)
        .toResultOr { "Selection ${this.selectionId} ciphertext missing" }

    if (ciphertext is Err) {
        return ciphertext
    }
    return Ok(
        EncryptedTally.Selection(
            this.selectionId,
            this.sequenceOrder,
            ciphertext.unwrap(),
        )
    )
}

/////////////////////////////////////////////////////////////////////////////////////////////////

fun EncryptedTally.publishProto() =
    electionguard.protogen.EncryptedTally(
        this.tallyId,
        this.contests.map { it.publishProto() },
        this.castBallotIds,
    )

private fun EncryptedTally.Contest.publishProto() =
    electionguard.protogen.EncryptedTallyContest(
        this.contestId,
        this.sequenceOrder,
        this.selections.map { it.publishProto() }
    )

private fun EncryptedTally.Selection.publishProto() =
    electionguard.protogen.EncryptedTallySelection(
        this.selectionId,
        this.sequenceOrder,
        this.encryptedVote.publishProto()
    )