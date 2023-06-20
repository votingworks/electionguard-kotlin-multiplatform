package electionguard.json

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.partition
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.core.GroupContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// LOOK can also be used for DecryptedBallot. Only difference is Contest.decryptedContestData
@Serializable
@SerialName("DecryptedTally")
data class DecryptedTallyJson(
    val object_id: String,
    val contests: List<DecryptedTallyContestJson>,
)

fun DecryptedTallyOrBallot.publish() = DecryptedTallyJson(
        this.id,
        this.contests.map { it.publish() },
    )

fun DecryptedTallyJson.import(group: GroupContext) : Result<DecryptedTallyOrBallot, String> {
    val (contests, cerrors) = this.contests.map { it.import(group) }.partition()
    if (cerrors.isNotEmpty()) {
        return Err(cerrors.joinToString("\n"))
    }
    return Ok(DecryptedTallyOrBallot(
        this.object_id,
        contests.sortedBy { it.contestId },
    ))
}

/////////////////////////

@Serializable
@SerialName("DecryptedTallyContest")
data class DecryptedTallyContestJson(
    val object_id: String,
    val selections: List<DecryptedTallySelectionJson>,
)

fun DecryptedTallyOrBallot.Contest.publish() = DecryptedTallyContestJson(
    this.contestId,
    this.selections.map { it.publish() },
)

fun DecryptedTallyContestJson.import(group: GroupContext) : Result<DecryptedTallyOrBallot.Contest, String> {
    val (selections, serrors) = this.selections.map { it.import(group) }.partition()
    if (serrors.isNotEmpty()) {
        return Err(serrors.joinToString("\n"))
    }
    return Ok(DecryptedTallyOrBallot.Contest(
        this.object_id,
        selections.sortedBy { it.selectionId },
    ))
}

/////////////////////////

@Serializable
@SerialName("DecryptedTallySelection")
data class DecryptedTallySelectionJson(
    val object_id: String,
    val tally: Int,
    val value: ElementModPJson,
    val message: ElGamalCiphertextJson,
    val proof: ChaumPedersenProofJson,
)

fun DecryptedTallyOrBallot.Selection.publish() = DecryptedTallySelectionJson(
    this.selectionId,
    this.tally,
    this.kExpTally.publish(),
    this.encryptedVote.publish(),
    this.proof.publish(),
)

fun DecryptedTallySelectionJson.import(group: GroupContext) : Result<DecryptedTallyOrBallot.Selection, String> {
    val value = this.value.import(group)
    val ciphertext = this.message.import(group)
    val proof = this.proof.import(group)
    if (value == null || ciphertext == null || proof == null) {
        return Err("Failed to import selection ${this.object_id}")
    }
    return Ok(DecryptedTallyOrBallot.Selection(
        this.object_id,
        this.tally,
        value,
        ciphertext,
        proof,
    ))
}
