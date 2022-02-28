package electionguard.protoconvert

import electionguard.ballot.CiphertextTally
import electionguard.core.GroupContext

fun electionguard.protogen.CiphertextTally.importCiphertextTally(groupContext: GroupContext): CiphertextTally {
    return CiphertextTally(
        this.tallyId,
        this.contests.associate { it.contestId to it.importContest(groupContext) }
    )
}

private fun electionguard.protogen.CiphertextTallyContest.importContest(groupContext: GroupContext): CiphertextTally.Contest {
    if (this.contestDescriptionHash == null) {
        throw IllegalStateException("contestDescriptionHash cant be null")
    }
    return CiphertextTally.Contest(
        this.contestId,
        this.sequenceOrder,
        this.contestDescriptionHash.importElementModQ(groupContext),
        this.selections.associate { it.selectionId to it.importSelection(groupContext) }
    )
}

private fun electionguard.protogen.CiphertextTallySelection.importSelection(groupContext: GroupContext): CiphertextTally.Selection {
    if (this.selectionDescriptionHash == null) {
        throw IllegalStateException("selectionDescriptionHash cant be null")
    }
    if (this.ciphertext == null) {
        throw IllegalStateException("ciphertext cant be null")
    }
    return CiphertextTally.Selection(
        this.selectionId,
        this.sequenceOrder,
        this.selectionDescriptionHash.importElementModQ(groupContext),
        this.ciphertext.importCiphertext(groupContext),
    )
}

/////////////////////////////////////////////////////////////////////////////////////////////////

fun CiphertextTally.publishCiphertextTally(): electionguard.protogen.CiphertextTally {
    return electionguard.protogen.CiphertextTally(
        this.tallyId,
        this.contests.values.map { it.publishContest() }
    )
}

private fun CiphertextTally.Contest.publishContest(): electionguard.protogen.CiphertextTallyContest {
    return electionguard.protogen.CiphertextTallyContest(
        this.contestId,
        this.sequenceOrder,
        this.contestDescriptionHash.publishElementModQ(),
        this.selections.values.map { it.publishSelection() }
    )
}

private fun CiphertextTally.Selection.publishSelection(): electionguard.protogen.CiphertextTallySelection {
    return electionguard.protogen.CiphertextTallySelection(
        this.selectionId,
        this.sequenceOrder,
        this.selectionDescriptionHash.publishElementModQ(),
        this.ciphertext.publishCiphertext()
    )
}