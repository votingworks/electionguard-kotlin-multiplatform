package electionguard.protoconvert

import electionguard.ballot.PlaintextBallot

class PlaintextBallotConvert {

    fun translateFromProto(ballot: electionguard.protogen.PlaintextBallot): PlaintextBallot {
        return PlaintextBallot(
            ballot.objectId,
            ballot.styleId,
            ballot.contests.map{ convertContest(it) }
            )
    }

    private fun convertContest(contest: electionguard.protogen.PlaintextBallotContest): PlaintextBallot.Contest {
        return PlaintextBallot.Contest(
            contest.contestId,
            contest.sequenceOrder,
            contest.ballotSelections.map{ convertSelection(it) }
        )
    }

    private fun convertSelection(selection: electionguard.protogen.PlaintextBallotSelection): PlaintextBallot.Selection {
        return PlaintextBallot.Selection(
            selection.selectionId,
            selection.sequenceOrder,
            selection.vote,
            selection.isPlaceholderSelection,
            convertExtendedData(selection.extendedData)
        )
    }

    private fun convertExtendedData(data: electionguard.protogen.ExtendedData?): PlaintextBallot.ExtendedData? {
        if (data == null) {
            return null
        }
        return PlaintextBallot.ExtendedData(
            data.value,
            data.length
        )
    }

    //////////////////////////////////////////////////////////////////////////////////////////////

    fun translateToProto(ballot: PlaintextBallot): electionguard.protogen.PlaintextBallot {
        return electionguard.protogen.PlaintextBallot(
            ballot.ballotId,
            ballot.ballotStyleId,
            ballot.contests.map{ convertContest(it) }
        )
    }

    private fun convertContest(contest: PlaintextBallot.Contest): electionguard.protogen.PlaintextBallotContest {
        return electionguard.protogen.PlaintextBallotContest(
            contest.contestId,
            contest.sequenceOrder,
            contest.ballotSelections.map{ convertSelection(it) }
        )
    }

    private fun convertSelection(selection: PlaintextBallot.Selection): electionguard.protogen.PlaintextBallotSelection {
        return electionguard.protogen.PlaintextBallotSelection(
            selection.selectionId,
            selection.sequenceOrder,
            selection.vote,
            selection.isPlaceholderSelection,
            convertExtendedData(selection.extendedData)
        )
    }

    private fun convertExtendedData(data: PlaintextBallot.ExtendedData?): electionguard.protogen.ExtendedData? {
        if (data == null) {
            return null
        }
        return electionguard.protogen.ExtendedData(
            data.value,
            data.length
        )
    }
}