package electionguard.protoconvert

import electionguard.ballot.PlaintextBallot

fun electionguard.protogen.PlaintextBallot.importPlaintextBallot(): PlaintextBallot {
    return PlaintextBallot(
        this.ballotId,
        this.ballotStyleId,
        this.contests.map { it.importContest() },
        if (this.errors.isEmpty()) null else this.errors,
    )
}

private fun electionguard.protogen.PlaintextBallotContest.importContest(): PlaintextBallot.Contest {
    return PlaintextBallot.Contest(
        this.contestId,
        this.sequenceOrder,
        this.selections.map { it.importSelection() }
    )
}

private fun electionguard.protogen.PlaintextBallotSelection.importSelection():
    PlaintextBallot.Selection {
        return PlaintextBallot.Selection(
            this.selectionId,
            this.sequenceOrder,
            this.vote,
            this.isPlaceholderSelection,
            this.extendedData?.let { this.extendedData.importExtendedData() }
        )
    }

private fun electionguard.protogen.ExtendedData.importExtendedData(): PlaintextBallot.ExtendedData {
    return PlaintextBallot.ExtendedData(this.value, this.length)
}

//////////////////////////////////////////////////////////////////////////////////////////////

fun PlaintextBallot.publishPlaintextBallot(): electionguard.protogen.PlaintextBallot {
    return electionguard.protogen
        .PlaintextBallot(
            this.ballotId,
            this.ballotStyleId,
            this.contests.map { it.publishContest() },
            this.errors ?: "",
        )
}

private fun PlaintextBallot.Contest.publishContest():
    electionguard.protogen.PlaintextBallotContest {
        return electionguard.protogen
            .PlaintextBallotContest(
                this.contestId,
                this.sequenceOrder,
                this.selections.map { it.publishSelection() }
            )
    }

private fun PlaintextBallot.Selection.publishSelection():
    electionguard.protogen.PlaintextBallotSelection {
        return electionguard.protogen
            .PlaintextBallotSelection(
                this.selectionId,
                this.sequenceOrder,
                this.vote,
                this.isPlaceholderSelection,
                this.extendedData?.let { this.extendedData.publishExtendedData() }
            )
    }

private fun PlaintextBallot.ExtendedData.publishExtendedData():
    electionguard.protogen.ExtendedData {
        return electionguard.protogen.ExtendedData(this.value, this.length)
    }