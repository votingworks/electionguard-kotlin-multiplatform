package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.partition
import com.github.michaelbull.result.toResultOr
import com.github.michaelbull.result.unwrap
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.core.GroupContext

fun GroupContext.importDecryptedTallyOrBallot(tally: electionguard.protogen.DecryptedTallyOrBallot?):
        Result<DecryptedTallyOrBallot, String> {
    if (tally == null) {
        return Err("Null DecryptedTallyOrBallot")
    }

    if (tally.contests.isEmpty()) {
        return Err("No contests in DecryptedTallyOrBallot")
    }

    val (contests, errors) = tally.contests.map { this.importContest(it) }.partition()
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(DecryptedTallyOrBallot(tally.id, contests.associateBy { it.contestId }))
}

private fun GroupContext.importContest(contest: electionguard.protogen.DecryptedContest):
        Result<DecryptedTallyOrBallot.Contest, String> {

    if (contest.selections.isEmpty()) {
        return Err("No selections in DecryptedContest")
    }

    val (selections, errors) = contest.selections.map { this.importSelection(it) }.partition()
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(DecryptedTallyOrBallot.Contest(
        contest.contestId,
        selections.associateBy { it.selectionId },
    null, // TODO
    ))
}

private fun GroupContext.importSelection(selection: electionguard.protogen.DecryptedSelection):
        Result<DecryptedTallyOrBallot.Selection, String> {
    val value = this.importElementModP(selection.value)
        .toResultOr { "DecryptedSelection ${selection.selectionId} value was malformed or missing" }
    val message = this.importCiphertext(selection.message)
        .toResultOr { "DecryptedSelection ${selection.selectionId} message was malformed or missing" }

    val errors = getAllErrors(value, message)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    if (selection.proof == null) {
        println("WHY?")
    }

    return Ok(
        DecryptedTallyOrBallot.Selection(
            selection.selectionId,
            selection.tally,
            value.unwrap(),
            message.unwrap(),
            this.importChaumPedersenProof(selection.proof)!!
        )
    )
}

//////////////////////////////////////////////////////////////////////////////////////////////

fun DecryptedTallyOrBallot.publishDecryptedTallyOrBallot(): electionguard.protogen.DecryptedTallyOrBallot {
    return electionguard.protogen
        .DecryptedTallyOrBallot(this.id, this.contests.values.map { it.publishContest() })
}

private fun DecryptedTallyOrBallot.Contest.publishContest(): electionguard.protogen.DecryptedContest {
    return electionguard.protogen
        .DecryptedContest(this.contestId, this.selections.values.map { it.publishSelection() })
}

private fun DecryptedTallyOrBallot.Selection.publishSelection():
        electionguard.protogen.DecryptedSelection {
    return electionguard.protogen
        .DecryptedSelection(
            this.selectionId,
            this.tally,
            this.value.publishElementModP(),
            this.message.publishCiphertext(),
            this.proof.publishChaumPedersenProof()
        )
}