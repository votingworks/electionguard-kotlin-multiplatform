package electionguard.testvectors

import electionguard.ballot.ManifestIF
import electionguard.ballot.PlaintextBallot
import kotlinx.serialization.Serializable

@Serializable
data class PlaintextBallotJson(
    val ballotId: String,
    val ballotStyle: String,
    val contests: List<PlaintextContestJson>,
)

@Serializable
data class PlaintextContestJson(
    val contestId: String,
    val sequenceOrder: Int,
    val votesAllowed: Int,
    val selections: List<PlaintextSelectionJson>,
)

@Serializable
data class PlaintextSelectionJson(
    val selectionId: String,
    val sequenceOrder: Int,
    val vote: Int,
)

fun PlaintextBallot.publishJson(): PlaintextBallotJson {
    val contests = this.contests.map { contest ->
        PlaintextContestJson(contest.contestId, contest.sequenceOrder, 1,
            contest.selections.map { PlaintextSelectionJson(it.selectionId, it.sequenceOrder, it.vote) })
    }
    return PlaintextBallotJson(this.ballotId, this.ballotStyle, contests)
}

fun PlaintextBallotJson.import(): PlaintextBallot {
    val contests = this.contests.map { contest ->
        PlaintextBallot.Contest(contest.contestId, contest.sequenceOrder,
            contest.selections.map { PlaintextBallot.Selection(it.selectionId, it.sequenceOrder, it.vote) })
    }
    return PlaintextBallot(this.ballotId, this.ballotStyle, contests)
}


class ManifestFacade(ballot : PlaintextBallotJson) : ManifestIF {
    override val contests : List<ContestFacade>
    init {
        this.contests = ballot.contests.map { bc ->
            ContestFacade(
                bc.contestId,
                bc.sequenceOrder,
                bc.votesAllowed,
                bc.selections.map { SelectionFacade(it.selectionId, it.sequenceOrder)}
            )
        }
    }

    override fun contestsForBallotStyle(ballotStyle : String) = contests

    class ContestFacade(
        override val contestId: String,
        override val sequenceOrder: Int,
        override val votesAllowed: Int,
        override val selections: List<ManifestIF.Selection>,
    ) : ManifestIF.Contest

    class SelectionFacade(
        override val selectionId: String,
        override val sequenceOrder: Int) : ManifestIF.Selection

}