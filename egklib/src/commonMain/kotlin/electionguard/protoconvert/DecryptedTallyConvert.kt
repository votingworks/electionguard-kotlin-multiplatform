package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.partition
import com.github.michaelbull.result.toResultOr
import com.github.michaelbull.result.unwrap
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.importContestData
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

    var decryptedContestData: DecryptedTallyOrBallot.DecryptedContestData? = null
    if (contest.decryptedContestData != null) {
        val decryptedContestDataResult =
            this.importDecryptedContestData(contest.contestId, contest.decryptedContestData)
        if (decryptedContestDataResult is Err) {
            return decryptedContestDataResult
        } else {
            decryptedContestData = decryptedContestDataResult.unwrap()
        }
    }

    return Ok(
        DecryptedTallyOrBallot.Contest(
            contest.contestId,
            selections.associateBy { it.selectionId },
            decryptedContestData,
        )
    )
}

private fun GroupContext.importDecryptedContestData(where: String,
                                                    dcontestData: electionguard.protogen.DecryptedContestData):
        Result<DecryptedTallyOrBallot.DecryptedContestData, String> {

    val contestData = importContestData(dcontestData.contestData)
    val encryptedContestData = this.importHashedCiphertext(dcontestData.encryptedContestData)
        .toResultOr { "$where: encryptedContestData was malformed" }
    val proof = this.importChaumPedersenProof(dcontestData.proof)
        .toResultOr { "$where: encryptedContestData was malformed" }
    val beta = this.importElementModP(dcontestData.beta)
        .toResultOr { "$where: encryptedContestData was malformed" }

    val errors = getAllErrors(contestData, encryptedContestData, proof, beta)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(
        DecryptedTallyOrBallot.DecryptedContestData(
            contestData.unwrap(),
            encryptedContestData.unwrap(),
            proof.unwrap(),
            beta.unwrap(),
        )
    )
}

private fun GroupContext.importSelection(selection: electionguard.protogen.DecryptedSelection):
        Result<DecryptedTallyOrBallot.Selection, String> {
    val value = this.importElementModP(selection.value)
        .toResultOr { "DecryptedSelection ${selection.selectionId} value was malformed or missing" }
    val message = this.importCiphertext(selection.message)
        .toResultOr { "DecryptedSelection ${selection.selectionId} message was malformed or missing" }
    val proof = this.importChaumPedersenProof(selection.proof)
        .toResultOr { "DecryptedSelection ${selection.selectionId} proof was malformed or missing" }

    val errors = getAllErrors(value, message, proof)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(
        DecryptedTallyOrBallot.Selection(
            selection.selectionId,
            selection.tally,
            value.unwrap(),
            message.unwrap(),
            proof.unwrap(),
        )
    )
}

//////////////////////////////////////////////////////////////////////////////////////////////

fun DecryptedTallyOrBallot.publishDecryptedTallyOrBallot() =
    electionguard.protogen.DecryptedTallyOrBallot(
        this.id,
        this.contests.values.map { it.publishContest() },
    )

private fun DecryptedTallyOrBallot.Contest.publishContest() =
    electionguard.protogen.DecryptedContest(
        this.contestId,
        this.selections.values.map { it.publishSelection() },
        this.decryptedContestData?.publishDecryptedContestData()
    )

private fun DecryptedTallyOrBallot.Selection.publishSelection() =
    electionguard.protogen.DecryptedSelection(
        this.selectionId,
        this.tally,
        this.value.publishElementModP(),
        this.message.publishCiphertext(),
        this.proof.publishChaumPedersenProof()
    )

private fun DecryptedTallyOrBallot.DecryptedContestData.publishDecryptedContestData() =
    electionguard.protogen.DecryptedContestData(
        this.contestData.publish(),
        this.encryptedContestData.publishHashedCiphertext(),
        this.proof.publishChaumPedersenProof(),
        this.beta.publishElementModP(),
    )