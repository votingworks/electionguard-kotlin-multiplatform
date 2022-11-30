package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
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
    return Ok(EncryptedTally(this.tallyId, contests))
}

private fun electionguard.protogen.EncryptedTallyContest.import(group: GroupContext):
        Result<EncryptedTally.Contest, String> {

    val contestHash = importUInt256(this.contestDescriptionHash)
        .toResultOr { "Contest ${this.contestId} description hash was malformed or missing" }
    val (selections, serrors) = this.selections.map { it.import(group) }.partition()

    val errors = getAllErrors(contestHash) + serrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }
    return Ok(EncryptedTally.Contest(this.contestId, this.sequenceOrder, contestHash.unwrap(), selections))
}

private fun electionguard.protogen.EncryptedTallySelection.import(group: GroupContext):
        Result<EncryptedTally.Selection, String> {

    val selectionDescriptionHash =
        importUInt256(this.selectionDescriptionHash).toResultOr { "Selection ${this.selectionId} description hash missing" }
    val ciphertext = group.importCiphertext(this.ciphertext)
        .toResultOr { "Selection ${this.selectionId} ciphertext missing" }

    val errors = getAllErrors(selectionDescriptionHash, ciphertext)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }
    return Ok(
        EncryptedTally.Selection(
            this.selectionId,
            this.sequenceOrder,
            selectionDescriptionHash.unwrap(),
            ciphertext.unwrap(),
        )
    )
}

/////////////////////////////////////////////////////////////////////////////////////////////////

fun EncryptedTally.publishProto() =
    electionguard.protogen.EncryptedTally(
        this.tallyId,
        this.contests.map { it.publishProto() }
    )

private fun EncryptedTally.Contest.publishProto() =
    electionguard.protogen.EncryptedTallyContest(
        this.contestId,
        this.sequenceOrder,
        this.contestDescriptionHash.publishProto(),
        this.selections.map { it.publishProto() }
    )

private fun EncryptedTally.Selection.publishProto() =
    electionguard.protogen.EncryptedTallySelection(
        this.selectionId,
        this.sequenceOrder,
        this.selectionDescriptionHash.publishProto(),
        this.ciphertext.publishProto()
    )