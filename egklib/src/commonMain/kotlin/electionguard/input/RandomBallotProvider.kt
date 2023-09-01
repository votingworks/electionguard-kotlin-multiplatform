package electionguard.input

import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import kotlin.random.Random

/** Create nballots randomly generated fake Ballots, used for testing.  */
class RandomBallotProvider(val manifest: Manifest, val nballots: Int = 11) {
    var addWriteIns = false
    var useSequential = false
    var sequentialId = 1

    fun withWriteIns() : RandomBallotProvider {
        this.addWriteIns = true
        return this
    }

    fun withSequentialIds(): RandomBallotProvider {
        this.useSequential = true
        return this
    }

    fun ballots(ballotStyleId: String? = null): List<PlaintextBallot> {
        val ballots: MutableList<PlaintextBallot> = ArrayList()
        val useStyle = ballotStyleId ?: manifest.ballotStyles[0].ballotStyleId
        for (i in 0 until nballots) {
            val ballotId = if (useSequential) "id-" + sequentialId++ else "id" + Random.nextInt()
            ballots.add(getFakeBallot(manifest, useStyle, ballotId))
        }
        return ballots
    }

    fun makeBallot(): PlaintextBallot {
        val useStyle = manifest.ballotStyles[0].ballotStyleId
        val ballotId = if (useSequential) "id-" + sequentialId++ else "id" + Random.nextInt()
        return getFakeBallot(manifest, useStyle, ballotId)
    }

    fun getFakeBallot(manifest: Manifest, ballotStyleId: String, ballotId: String): PlaintextBallot {
        val contests: MutableList<PlaintextBallot.Contest> = ArrayList()
        val ballotStyle = manifest.ballotStyles.find { it.ballotStyleId == ballotStyleId }
        for (contestp in manifest.contests) {
            if (ballotStyle!!.geopoliticalUnitIds.contains(contestp.geopoliticalUnitId)) {
                contests.add(makeContestFrom(contestp))
            }
        }
        return PlaintextBallot(ballotId, ballotStyleId, contests)
    }

    fun makeContestFrom(contest: Manifest.ContestDescription): PlaintextBallot.Contest {
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
