package electionguard.json

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.partition
import electionguard.ballot.EncryptedTally
import electionguard.ballot.PlaintextBallot
import electionguard.core.GroupContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PlaintextBallot")
data class PlaintextBallotJson(
    val object_id: String,
    val style_id: String,
    val contests: List<PlaintextBallotContestJson>,
)

fun PlaintextBallot.publish() = PlaintextBallotJson(
        this.ballotId,
        this.ballotStyleId,
        this.contests.map { it.publish() },
    )

fun PlaintextBallotJson.import() : Result<PlaintextBallot, String> {
    val (contests, cerrors) = this.contests.map { it.import() }.partition()
    if (cerrors.isNotEmpty()) {
        return Err(cerrors.joinToString("\n"))
    }
    return Ok(PlaintextBallot(
        this.object_id,
        this.style_id,
        contests,
    ))
}

/////////////////////////

@Serializable
@SerialName("PlaintextBallotContest")
data class PlaintextBallotContestJson(
    val object_id: String,
    val sequence_order: Int,
    val selections: List<PlaintextBallotSelectionJson>,
)

fun PlaintextBallot.Contest.publish() = PlaintextBallotContestJson(
    this.contestId,
    this.sequenceOrder,
    this.selections.map { it.publish() },
)

fun PlaintextBallotContestJson.import() : Result<PlaintextBallot.Contest, String> {
    val (selections, serrors) = this.selections.map { it.import() }.partition()
    if (serrors.isNotEmpty()) {
        return Err(serrors.joinToString("\n"))
    }
    return Ok(PlaintextBallot.Contest(
        this.object_id,
        this.sequence_order,
        selections,
    ))
}

/////////////////////////

@Serializable
@SerialName("PlaintextBallotSelection")
data class PlaintextBallotSelectionJson(
    val object_id: String,
    val sequence_order: Int,
    val vote: Int,
)

fun PlaintextBallot.Selection.publish() = PlaintextBallotSelectionJson(
    this.selectionId,
    this.sequenceOrder,
    this.vote,
)

fun PlaintextBallotSelectionJson.import() : Result<PlaintextBallot.Selection, String> {
    return Ok(PlaintextBallot.Selection(
        this.object_id,
        this.sequence_order,
        this.vote,
    ))
}
