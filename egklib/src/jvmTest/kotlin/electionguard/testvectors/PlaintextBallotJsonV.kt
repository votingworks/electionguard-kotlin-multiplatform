package electionguard.testvectors

import electionguard.ballot.ManifestIF
import electionguard.ballot.PlaintextBallot
import kotlinx.serialization.Serializable

@Serializable
data class PlaintextBallotJsonV(
    val ballot_id: String,
    val ballot_style: String,
    val contests: List<PlaintextContestJsonV>,
)

@Serializable
data class PlaintextContestJsonV(
    val contest_id: String,
    val sequence_order: Int,
    val votes_allowed: Int,
    val selections: List<PlaintextSelectionJsonV>,
)

@Serializable
data class PlaintextSelectionJsonV(
    val selection_id: String,
    val sequence_order: Int,
    val vote: Int,
)

fun PlaintextBallot.publishJsonE(): PlaintextBallotJsonV {
    val contests = this.contests.map { contest ->
        PlaintextContestJsonV(contest.contestId, contest.sequenceOrder, 1,
            contest.selections.map { PlaintextSelectionJsonV(it.selectionId, it.sequenceOrder, it.vote) })
    }
    return PlaintextBallotJsonV(this.ballotId, this.ballotStyle, contests)
}

fun PlaintextBallotJsonV.import(): PlaintextBallot {
    val contests = this.contests.map { contest ->
        PlaintextBallot.Contest(contest.contest_id, contest.sequence_order,
            contest.selections.map { PlaintextBallot.Selection(it.selection_id, it.sequence_order, it.vote) })
    }
    return PlaintextBallot(this.ballot_id, this.ballot_style, contests)
}

// create a ManifestIF from PlaintextBallotJson
class PlaintextBallotJsonManifestFacade(ballot : PlaintextBallotJsonV) : ManifestIF {
    override val contests : List<ContestFacade>
    init {
        this.contests = ballot.contests.map { bc ->
            ContestFacade(
                bc.contest_id,
                bc.sequence_order,
                bc.selections.map { SelectionFacade(it.selection_id, it.sequence_order) },
                bc.votes_allowed,
            )
        }
    }

    override fun contestsForBallotStyle(ballotStyle : String) = contests
    override fun contestLimit(contestId: String): Int {
        return contests.find{ it.contestId == contestId }!!.votesAllowed
    }
    override fun optionLimit(contestId : String) : Int {
        return contests.find{ it.contestId == contestId }!!.optionLimit
    }

    class ContestFacade(
        override val contestId: String,
        override val sequenceOrder: Int,
        override val selections: List<ManifestIF.Selection>,
        val votesAllowed: Int = 1,
        val optionLimit: Int = 1,
    ) : ManifestIF.Contest

    class SelectionFacade(
        override val selectionId: String,
        override val sequenceOrder: Int
    ) : ManifestIF.Selection

}