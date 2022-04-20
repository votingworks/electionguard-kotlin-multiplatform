package electionguard.encrypt

import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot

fun makeBallot(election: Manifest, ballotStyleId : String, contestIdx: Int, selectionIdx: Int): PlaintextBallot {
    val contests: MutableList<PlaintextBallot.Contest> = ArrayList()
    contests.add(makeContest(election.contests.get(contestIdx), selectionIdx))
    return PlaintextBallot("id", ballotStyleId, contests)
}

fun makeContest(contest: Manifest.ContestDescription, selectionIdx: Int): PlaintextBallot.Contest {
    val selections: MutableList<PlaintextBallot.Selection> = ArrayList()
    selections.add(makeSelection(contest.selections.get(selectionIdx)))

        return PlaintextBallot.Contest(
            contest.contestId,
            contest.sequenceOrder,
            selections
        )
}

fun makeSelection(selection: Manifest.SelectionDescription): PlaintextBallot.Selection {
    return PlaintextBallot.Selection(
        selection.selectionId, selection.sequenceOrder,
        1, null
    )
}