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

fun electionguard.protogen.DecryptedTallyOrBallot.import(group: GroupContext):
        Result<DecryptedTallyOrBallot, String> {

    if (this.contests.isEmpty()) {
        return Err("No contests in DecryptedTallyOrBallot")
    }

    val (contests, errors) = this.contests.map { it.import(group) }.partition()
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(DecryptedTallyOrBallot(
        this.id,
        contests.sortedBy { it.contestId }
    ))
}

private fun electionguard.protogen.DecryptedContest.import(group: GroupContext):
        Result<DecryptedTallyOrBallot.Contest, String> {

    if (this.selections.isEmpty()) {
        return Err("No selections in DecryptedContest")
    }
    val (selections, errors) = this.selections.map { it.import(group) }.partition()
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    var decryptedContestData: DecryptedTallyOrBallot.DecryptedContestData? = null
    if (this.decryptedContestData != null) {
        val decryptedContestDataResult = this.decryptedContestData.import(this.contestId, group)
        if (decryptedContestDataResult is Err) {
            return decryptedContestDataResult
        } else {
            decryptedContestData = decryptedContestDataResult.unwrap()
        }
    }

    return Ok(
        DecryptedTallyOrBallot.Contest(
            this.contestId,
            selections.sortedBy { it.selectionId },
            decryptedContestData,
        )
    )
}

private fun electionguard.protogen.DecryptedContestData.import(where: String, group: GroupContext):
        Result<DecryptedTallyOrBallot.DecryptedContestData, String> {

    val contestData = importContestData(this.contestData)
    val encryptedContestData = group.importHashedCiphertext(this.encryptedContestData)
        .toResultOr { "$where: encryptedContestData was malformed" }
    val proof = group.importChaumPedersenProof(this.proof)
        .toResultOr { "$where: encryptedContestData was malformed" }
    val beta = group.importElementModP(this.beta)
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

private fun electionguard.protogen.DecryptedSelection.import(group: GroupContext):
        Result<DecryptedTallyOrBallot.Selection, String> {
    val value = group.importElementModP(this.value)
        .toResultOr { "DecryptedSelection ${this.selectionId} value was malformed or missing" }
    val ciphertext = group.importCiphertext(this.ciphertext)
        .toResultOr { "DecryptedSelection ${this.selectionId} ciphertext was malformed or missing" }
    val proof = group.importChaumPedersenProof(this.proof)
        .toResultOr { "DecryptedSelection ${this.selectionId} proof was malformed or missing" }

    val errors = getAllErrors(value, ciphertext, proof)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(
        DecryptedTallyOrBallot.Selection(
            this.selectionId,
            this.tally,
            value.unwrap(),
            ciphertext.unwrap(),
            proof.unwrap(),
        )
    )
}

//////////////////////////////////////////////////////////////////////////////////////////////

fun DecryptedTallyOrBallot.publishProto() =
    electionguard.protogen.DecryptedTallyOrBallot(
        this.id,
        this.contests.map { it.publishProto() },
    )

private fun DecryptedTallyOrBallot.Contest.publishProto() =
    electionguard.protogen.DecryptedContest(
        this.contestId,
        this.selections.map { it.publishProto() },
        this.decryptedContestData?.publishProto()
    )

private fun DecryptedTallyOrBallot.Selection.publishProto() =
    electionguard.protogen.DecryptedSelection(
        this.selectionId,
        this.tally,
        this.value.publishProto(),
        this.ciphertext.publishProto(),
        this.proof.publishProto()
    )

private fun DecryptedTallyOrBallot.DecryptedContestData.publishProto() =
    electionguard.protogen.DecryptedContestData(
        this.contestData.publish(),
        this.encryptedContestData.publishProto(),
        this.proof.publishProto(),
        this.beta.publishProto(),
    )