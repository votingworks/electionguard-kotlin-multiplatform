package electionguard.protoconvert

import electionguard.ballot.CiphertextTally
import electionguard.core.GroupContext
import electionguard.protogen.CiphertextTallyContest
import electionguard.protogen.CiphertextTallySelection

data class CiphertextTallyConvert(val groupContext: GroupContext) {

    fun translateFromProto(proto: electionguard.protogen.CiphertextTally): CiphertextTally {
        return CiphertextTally(
            proto.objectId,
            proto.contests.map{ (key, value) -> key to convertContest(value ?:
            throw IllegalArgumentException("PlaintextTallyContest cannot be null")) }
                .toMap()
        )
    }

    private fun convertContest(proto: CiphertextTallyContest): CiphertextTally.Contest {
        return CiphertextTally.Contest(
            proto.objectId,
            proto.sequenceOrder,
            convertElementModQ(proto.descriptionHash?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            proto.tallySelections.map{ (key, value) -> key to convertSelection(value ?:
            throw IllegalArgumentException("PlaintextTallySelection cannot be null")) }
                .toMap()
        )
    }

    private fun convertSelection(proto: CiphertextTallySelection): CiphertextTally.Selection {
        return CiphertextTally.Selection(
            proto.objectId,
            proto.sequenceOrder,
            convertElementModQ(proto.descriptionHash?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            convertCiphertext(proto.ciphertext?: throw IllegalArgumentException("Selection message cannot be null"), groupContext),
        )
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////

    fun translateToProto(tally: CiphertextTally): electionguard.protogen.CiphertextTally {
        return electionguard.protogen.CiphertextTally(
            tally.objectId,
            tally.contests.map{ convertContest(it.value) }
        )
    }

    private fun convertContest(contest: CiphertextTally.Contest): electionguard.protogen.CiphertextTally.ContestsEntry {
        return electionguard.protogen.CiphertextTally.ContestsEntry(
            contest.objectId,
            electionguard.protogen.CiphertextTallyContest(
                contest.objectId,
                contest.sequenceOrder,
                convertElementModQ(contest.contestDescriptionHash),
                contest.selections.values.map{ convertSelection(it) }
            )
        )
    }

    private fun convertSelection(selection: CiphertextTally.Selection): electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry {
        return electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry(
            selection.objectId,
            electionguard.protogen.CiphertextTallySelection(
                selection.objectId,
                selection.sequenceOrder,
                convertElementModQ(selection.descriptionHash),
                convertCiphertext(selection.ciphertext)
            )
        )
    }
}