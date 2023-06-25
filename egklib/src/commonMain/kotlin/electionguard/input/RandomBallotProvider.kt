package electionguard.input

import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import kotlin.random.Random

/** Create nballots randomly generated fake Ballots, used for testing.  */
class RandomBallotProvider(val manifest: Manifest, val nballots: Int = 11, val addWriteIns: Boolean = false) {

    fun ballots(ballotStyleId: String? = null): List<PlaintextBallot> {
        val ballots: MutableList<PlaintextBallot> = ArrayList()
        val useStyle = ballotStyleId ?: manifest.ballotStyles[0].ballotStyleId
        for (i in 0 until nballots) {
            val ballotId = "id" + Random.nextInt()
            ballots.add(getFakeBallot(manifest, useStyle, ballotId))
        }
        return ballots
    }

    fun getFakeBallot(manifest: Manifest, ballotStyleId: String, ballotId: String): PlaintextBallot {
        val contests: MutableList<PlaintextBallot.Contest> = ArrayList()
        for (contestp in manifest.contests) {
            contests.add(getRandomContestFrom(contestp))
        }
        return PlaintextBallot(ballotId, ballotStyleId, contests)
    }

    fun getRandomContestFrom(contest: Manifest.ContestDescription): PlaintextBallot.Contest {
        var voted = 0
        val selections: MutableList<PlaintextBallot.Selection> = ArrayList()
        val nselections = contest.selections.size

        for (selection_description in contest.selections) {
            val selection: PlaintextBallot.Selection = getRandomVoteForSelection(selection_description, nselections, voted < contest.votesAllowed)
            selections.add(selection)
            voted += selection.vote
        }
        val choice = Random.nextInt(nselections)
        val writeins = if (!addWriteIns || choice != 0) emptyList() else {
            listOf("writein")
        }
        return PlaintextBallot.Contest(
            contest.contestId,
            contest.sequenceOrder,
            selections,
            writeins,
        )
    }

    companion object {
        fun getRandomVoteForSelection(description: Manifest.SelectionDescription, nselections: Int, moreAllowed : Boolean): PlaintextBallot.Selection {
            val choice = Random.nextInt(nselections)
            return PlaintextBallot.Selection(
                description.selectionId, description.sequenceOrder,
                if (choice == 0 && moreAllowed) 1 else 0,
            )
        }
    }
}