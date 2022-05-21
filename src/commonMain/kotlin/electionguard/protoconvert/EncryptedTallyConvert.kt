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

fun GroupContext.importEncryptedTally(tally: electionguard.protogen.EncryptedTally?):
        Result<EncryptedTally, String> {
    if (tally == null) {
        return Err("Null EncryptedTally")
    }

    val (contests, errors) = tally.contests.map { this.importContest(it) }.partition()
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(EncryptedTally(tally.tallyId, contests))
}

private fun GroupContext.importContest(contest: electionguard.protogen.EncryptedTallyContest):
        Result<EncryptedTally.Contest, String> {

    val contestHash = importUInt256(contest.contestDescriptionHash)
        .toResultOr {"Contest ${contest.contestId} description hash was malformed or missing"}
    val (selections, serrors) = contest.selections.map { this.importSelection(it) }.partition()

    val errors = getAllErrors(contestHash) + serrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(EncryptedTally.Contest(contest.contestId, contest.sequenceOrder, contestHash.unwrap(), selections))
}

private fun GroupContext.importSelection(selection: electionguard.protogen.EncryptedTallySelection):
        Result<EncryptedTally.Selection, String> {

    val selectionDescriptionHash = importUInt256(selection.selectionDescriptionHash)
    val ciphertext = this.importCiphertext(selection.ciphertext)

    if (selectionDescriptionHash == null || ciphertext == null) {
        return Err("Selection ${selection.selectionId} description hash or ciphertext missing")
    }

    return Ok(EncryptedTally.Selection(
        selection.selectionId,
        selection.sequenceOrder,
        selectionDescriptionHash,
        ciphertext
    ))
}

/////////////////////////////////////////////////////////////////////////////////////////////////

fun EncryptedTally.publishEncryptedTally(): electionguard.protogen.EncryptedTally {
    return electionguard.protogen
        .EncryptedTally(this.tallyId, this.contests.map { it.publishContest() })
}

private fun EncryptedTally.Contest.publishContest():
    electionguard.protogen.EncryptedTallyContest {
        return electionguard.protogen
            .EncryptedTallyContest(
                this.contestId,
                this.sequenceOrder,
                this.contestDescriptionHash.publishUInt256(),
                this.selections.map { it.publishSelection() }
            )
    }

private fun EncryptedTally.Selection.publishSelection():
    electionguard.protogen.EncryptedTallySelection {
        return electionguard.protogen
            .EncryptedTallySelection(
                this.selectionId,
                this.sequenceOrder,
                this.selectionDescriptionHash.publishUInt256(),
                this.ciphertext.publishCiphertext()
            )
    }