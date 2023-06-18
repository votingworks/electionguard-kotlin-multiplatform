package electionguard.protoconvert

import electionguard.ballot.PlaintextBallot

fun electionguard.protogen.PlaintextBallot.import() =
    PlaintextBallot(
        this.ballotId,
        this.ballotStyleId,
        this.contests.map { it.import() },
        this.errors.ifEmpty { null },
    )

private fun electionguard.protogen.PlaintextBallotContest.import() =
    PlaintextBallot.Contest(
        this.contestId,
        this.sequenceOrder,
        this.selections.map { it.import() },
        this.writeIns,
    )

private fun electionguard.protogen.PlaintextBallotSelection.import() =
    PlaintextBallot.Selection(
        this.selectionId,
        this.sequenceOrder,
        this.vote,
    )

//////////////////////////////////////////////////////////////////////////////////////////////

fun PlaintextBallot.publishProto() =
    electionguard.protogen.PlaintextBallot(
        this.ballotId,
        this.ballotStyle,
        this.contests.map { it.publishProto() },
        this.errors ?: "",
    )

private fun PlaintextBallot.Contest.publishProto() =
    electionguard.protogen.PlaintextBallotContest(
            this.contestId,
            this.sequenceOrder,
            this.selections.map { it.publishProto() },
            this.writeIns,
        )

private fun PlaintextBallot.Selection.publishProto() =
    electionguard.protogen.PlaintextBallotSelection(
            this.selectionId,
            this.sequenceOrder,
            this.vote,
        )