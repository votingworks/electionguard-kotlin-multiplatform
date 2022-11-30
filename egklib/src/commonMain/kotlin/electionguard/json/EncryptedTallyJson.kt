package electionguard.json

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.partition
import electionguard.ballot.EncryptedTally
import electionguard.core.GroupContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("EncryptedTally")
data class EncryptedTallyJson(
    val object_id: String,
    val contests: List<EncryptedTallyContestJson>,
)

fun EncryptedTally.publish() = EncryptedTallyJson(
        this.tallyId,
        this.contests.map { it.publish() },
    )

fun EncryptedTallyJson.import(group: GroupContext) : Result<EncryptedTally, String> {
    val (contests, cerrors) = this.contests.map { it.import(group) }.partition()
    if (cerrors.isNotEmpty()) {
        return Err(cerrors.joinToString("\n"))
    }
    return Ok(EncryptedTally(
        this.object_id,
        contests,
    ))
}

/////////////////////////

@Serializable
@SerialName("EncryptedTallyContest")
data class EncryptedTallyContestJson(
    val object_id: String,
    val sequence_order: Int,
    val description_hash: UInt256Json,
    val selections: List<EncryptedTallySelectionJson>,
)

fun EncryptedTally.Contest.publish() = EncryptedTallyContestJson(
    this.contestId,
    this.sequenceOrder,
    this.contestDescriptionHash.publish(),
    this.selections.map { it.publish() },
)

fun EncryptedTallyContestJson.import(group: GroupContext) : Result<EncryptedTally.Contest, String> {
    val contestHash = this.description_hash.import()
    if (contestHash == null) {
        return Err("Failed to import contest ${this.object_id}")
    }
    val (selections, serrors) = this.selections.map { it.import(group) }.partition()
    if (serrors.isNotEmpty()) {
        return Err(serrors.joinToString("\n"))
    }
    return Ok(EncryptedTally.Contest(
        this.object_id,
        this.sequence_order,
        contestHash,
        selections,
    ))
}

/////////////////////////

@Serializable
@SerialName("EncryptedTallySelection")
data class EncryptedTallySelectionJson(
    val object_id: String,
    val sequence_order: Int,
    val description_hash: UInt256Json,
    val ciphertext: ElGamalCiphertextJson,
)

fun EncryptedTally.Selection.publish() = EncryptedTallySelectionJson(
    this.selectionId,
    this.sequenceOrder,
    this.selectionDescriptionHash.publish(),
    this.ciphertext.publish(),
)

fun EncryptedTallySelectionJson.import(group: GroupContext) : Result<EncryptedTally.Selection, String> {
    val selectionHash = this.description_hash.import()
    val ciphertext = this.ciphertext.import(group)
    if (selectionHash == null || ciphertext == null) {
        return Err("Failed to import selection ${this.object_id}")
    }
    return Ok(EncryptedTally.Selection(
        this.object_id,
        this.sequence_order,
        selectionHash,
        ciphertext,
    ))
}
