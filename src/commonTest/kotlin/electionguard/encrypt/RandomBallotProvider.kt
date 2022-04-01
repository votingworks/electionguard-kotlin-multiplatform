package electionguard.encrypt

import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import kotlin.random.Random

/** Create nballots randomly generated fake Ballots.  */
class RandomBallotProvider(election: Manifest, nballots: Int?) {
    private val nballots: Int
    private val election: Manifest

    init {
        this.election = election
        this.nballots = if (nballots != null && nballots > 0) nballots else 11
    }

    fun ballots(): Iterable<PlaintextBallot> {
        val ballotFactory = BallotFactory()
        val ballots: MutableList<PlaintextBallot> = ArrayList()
        for (i in 0 until nballots) {
            val ballot_id = "ballot-id-" + Random.nextInt()
            ballots.add(ballotFactory.get_fake_ballot(election, ballot_id))
        }
        return ballots
    }

    private class BallotFactory {
        fun get_fake_ballot(manifest: Manifest, ballot_id: String): PlaintextBallot {
            val contests: MutableList<PlaintextBallot.Contest> = ArrayList()
            for (contestp in manifest.contests) {
                contests.add(get_random_contest_from(contestp))
            }
            return PlaintextBallot(ballot_id, "ballotStyleId", contests)
        }

        fun get_random_contest_from(contest: Manifest.ContestDescription): PlaintextBallot.Contest {
            var voted = 0
            val selections: MutableList<PlaintextBallot.Selection> = ArrayList()
            for (selection_description in contest.selections) {
                val selection: PlaintextBallot.Selection = get_random_selection_from(selection_description)
                voted += selection.vote
                if (voted <= contest.votesAllowed) {
                    selections.add(selection)
                }
            }
            return PlaintextBallot.Contest(
                contest.contestId,
                contest.sequenceOrder,
                selections
            )
        }

        companion object {
            fun get_random_selection_from(description: Manifest.SelectionDescription): PlaintextBallot.Selection {
                val choice: Boolean = Random.nextBoolean()
                return PlaintextBallot.Selection(
                    description.selectionId, description.sequenceOrder,
                    if (choice) 1 else 0, false, null
                )
            }
        }
    }
}