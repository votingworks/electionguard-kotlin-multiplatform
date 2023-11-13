package electionguard.json2

import electionguard.ballot.PlaintextBallot
import kotlinx.serialization.Serializable

@Serializable
data class PlaintextBallotJson(
    val ballot_id: String,
    val ballot_style: String,
    val contests: List<PlaintextContestJson>,
    val sn: Long?, // must be > 0
    val errors: String?, // error messages from processing, eg when invalid
)

@Serializable
data class PlaintextContestJson(
    val contest_id: String,
    val sequence_order: Int,
    val selections: List<PlaintextSelectionJson>,
    val write_ins: List<String>,
)

@Serializable
data class PlaintextSelectionJson(
    val selection_id: String,
    val sequence_order: Int,
    val vote: Int,
)

fun PlaintextBallot.publishJson(): PlaintextBallotJson {
    val contests = this.contests.map { contest ->
        PlaintextContestJson(
            contest.contestId,
            contest.sequenceOrder,
            contest.selections.map { PlaintextSelectionJson(it.selectionId, it.sequenceOrder, it.vote) },
            contest.writeIns,
        )
    }
    return PlaintextBallotJson(this.ballotId, this.ballotStyle, contests, this.sn, this.errors)
}

fun PlaintextBallotJson.import(): PlaintextBallot {
    val contests = this.contests.map { contest ->
        PlaintextBallot.Contest(
            contest.contest_id,
            contest.sequence_order,
            contest.selections.map { PlaintextBallot.Selection(it.selection_id, it.sequence_order, it.vote) },
            contest.write_ins,
        )
    }
    return PlaintextBallot(this.ballot_id, this.ballot_style, contests, this.sn, this.errors)
}