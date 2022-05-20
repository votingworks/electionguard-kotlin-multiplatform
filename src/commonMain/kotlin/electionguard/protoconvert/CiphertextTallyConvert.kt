package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.partition
import com.github.michaelbull.result.toResultOr
import com.github.michaelbull.result.unwrap
import electionguard.ballot.CiphertextTally
import electionguard.core.GroupContext

fun GroupContext.importCiphertextTally(tally: electionguard.protogen.CiphertextTally?):
        Result<CiphertextTally, String> {
    if (tally == null) {
        return Err("Null CiphertextTally")
    }

    val (contests, errors) = tally.contests.map { this.importContest(it) }.partition()
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(CiphertextTally(tally.tallyId, contests))
}

private fun GroupContext.importContest(contest: electionguard.protogen.CiphertextTallyContest):
        Result<CiphertextTally.Contest, String> {

    val contestHash = importUInt256(contest.contestDescriptionHash)
        .toResultOr {"Contest ${contest.contestId} description hash was malformed or missing"}
    val (selections, serrors) = contest.selections.map { this.importSelection(it) }.partition()

    val errors = getAllErrors(contestHash) + serrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(CiphertextTally.Contest(contest.contestId, contest.sequenceOrder, contestHash.unwrap(), selections))
}

private fun GroupContext.importSelection(selection: electionguard.protogen.CiphertextTallySelection):
        Result<CiphertextTally.Selection, String> {

    val selectionDescriptionHash = importUInt256(selection.selectionDescriptionHash)
    val ciphertext = this.importCiphertext(selection.ciphertext)

    if (selectionDescriptionHash == null || ciphertext == null) {
        return Err("Selection ${selection.selectionId} description hash or ciphertext missing")
    }

    return Ok(CiphertextTally.Selection(
        selection.selectionId,
        selection.sequenceOrder,
        selectionDescriptionHash,
        ciphertext
    ))
}

/////////////////////////////////////////////////////////////////////////////////////////////////

fun CiphertextTally.publishCiphertextTally(): electionguard.protogen.CiphertextTally {
    return electionguard.protogen
        .CiphertextTally(this.tallyId, this.contests.map { it.publishContest() })
}

private fun CiphertextTally.Contest.publishContest():
    electionguard.protogen.CiphertextTallyContest {
        return electionguard.protogen
            .CiphertextTallyContest(
                this.contestId,
                this.sequenceOrder,
                this.contestDescriptionHash.publishUInt256(),
                this.selections.map { it.publishSelection() }
            )
    }

private fun CiphertextTally.Selection.publishSelection():
    electionguard.protogen.CiphertextTallySelection {
        return electionguard.protogen
            .CiphertextTallySelection(
                this.selectionId,
                this.sequenceOrder,
                this.selectionDescriptionHash.publishUInt256(),
                this.ciphertext.publishCiphertext()
            )
    }