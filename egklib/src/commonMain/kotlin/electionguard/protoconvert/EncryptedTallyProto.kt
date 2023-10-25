package electionguard.protoconvert

import com.github.michaelbull.result.*
import electionguard.ballot.EncryptedTally
import electionguard.core.GroupContext

fun electionguard.protogen.EncryptedTally.import(group: GroupContext): Result<EncryptedTally, String> {
    val electionId = importUInt256(this.electionId).toResultOr { "EncryptedTally ${this.tallyId} electionId was malformed or missing" }
    val (contests, cerrors) = this.contests.map { it.import(group) }.partition()

    val errors = getAllErrors(electionId) + cerrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(EncryptedTally(this.tallyId, contests, this.castBallotIds, electionId.unwrap()))
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
        this.electionId.publishProto(),
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