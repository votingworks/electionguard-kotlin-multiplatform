package electionguard.protoconvert

import electionguard.ballot.CiphertextTally
import electionguard.core.GroupContext
import electionguard.protogen.CiphertextTallyContest
import electionguard.protogen.CiphertextTallySelection

data class CiphertextTallyConvert(val groupContext: GroupContext) {

    fun translateFromProto(proto: electionguard.protogen.CiphertextTally): CiphertextTally {
        return CiphertextTally(
            proto.tallyId,
            proto.contests.associate{ it.contestId to convertContest(it) }
        )
    }

    private fun convertContest(proto: CiphertextTallyContest): CiphertextTally.Contest {
        return CiphertextTally.Contest(
            proto.contestId,
            proto.sequenceOrder,
            convertElementModQ(proto.contestDescriptionHash?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            proto.selections.associate{ it.selectionId to convertSelection(it) }
        )
    }

    private fun convertSelection(proto: CiphertextTallySelection): CiphertextTally.Selection {
        return CiphertextTally.Selection(
            proto.selectionId,
            proto.sequenceOrder,
            convertElementModQ(proto.selectionDescriptionHash?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            convertCiphertext(proto.ciphertext?: throw IllegalArgumentException("Selection message cannot be null"), groupContext),
        )
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////

    fun translateToProto(tally: CiphertextTally): electionguard.protogen.CiphertextTally {
        return electionguard.protogen.CiphertextTally(
            tally.tallyId,
            tally.contests.map{ convertContest(it.value) }
        )
    }

    private fun convertContest(contest: CiphertextTally.Contest): electionguard.protogen.CiphertextTallyContest {
        return electionguard.protogen.CiphertextTallyContest(
                contest.contestId,
                contest.sequenceOrder,
                convertElementModQ(contest.contestDescriptionHash),
                contest.selections.values.map{ convertSelection(it) }
        )
    }

    private fun convertSelection(selection: CiphertextTally.Selection): electionguard.protogen.CiphertextTallySelection {
        return electionguard.protogen.CiphertextTallySelection(
                selection.selectionId,
                selection.sequenceOrder,
                convertElementModQ(selection.selectionDescriptionHash),
                convertCiphertext(selection.ciphertext)
        )
    }
}