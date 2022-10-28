package electionguard.protoconvert

import electionguard.ballot.PlaintextBallot

fun importPlaintextBallot(ballot: electionguard.protogen.PlaintextBallot) =
    PlaintextBallot(
        ballot.ballotId,
        ballot.ballotStyleId,
        ballot.contests.map { importContest(it) },
        ballot.errors.ifEmpty { null },
    )

private fun importContest(contest: electionguard.protogen.PlaintextBallotContest) =
    PlaintextBallot.Contest(
        contest.contestId,
        contest.sequenceOrder,
        contest.selections.map { importSelection(it) },
        contest.writeIns,
    )

private fun importSelection(selection: electionguard.protogen.PlaintextBallotSelection) =
    PlaintextBallot.Selection(
        selection.selectionId,
        selection.sequenceOrder,
        selection.vote,
    )

//////////////////////////////////////////////////////////////////////////////////////////////

fun PlaintextBallot.publishPlaintextBallot() =
    electionguard.protogen.PlaintextBallot(
        this.ballotId,
        this.ballotStyleId,
        this.contests.map { it.publishContest() },
        this.errors ?: "",
    )

private fun PlaintextBallot.Contest.publishContest() =
    electionguard.protogen.PlaintextBallotContest(
            this.contestId,
            this.sequenceOrder,
            this.selections.map { it.publishSelection() },
            this.writeIns,
        )

private fun PlaintextBallot.Selection.publishSelection() =
    electionguard.protogen.PlaintextBallotSelection(
            this.selectionId,
            this.sequenceOrder,
            this.vote,
        )