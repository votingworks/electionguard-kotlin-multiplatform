package electionguard.protoconvert

import electionguard.ballot.CiphertextTally
import electionguard.core.GroupContext
import electionguard.core.noNullValuesOrNull
import mu.KotlinLogging

private val logger = KotlinLogging.logger("CommonConvert")

fun electionguard.protogen.CiphertextTally.importCiphertextTally(
    groupContext: GroupContext
): CiphertextTally? {
    val contestMap =
        this.contests
            .associate { it.contestId to it.importContest(groupContext) }
            .noNullValuesOrNull()

    if (contestMap == null) {
        logger.error { "Failed to convert CiphertextTally's contest map" }
        return null
    }

    return CiphertextTally(this.tallyId, contestMap)
}

private fun electionguard.protogen.CiphertextTallyContest.importContest(
    groupContext: GroupContext
): CiphertextTally.Contest? {

    val contestHash = groupContext.importUInt256(this.contestDescriptionHash)

    if (contestHash == null) {
        logger.error { "Contest description hash was malformed or out of bounds" }
        return null
    }

    val selectionMap =
        this.selections
            .associate { it.selectionId to it.importSelection(groupContext) }
            .noNullValuesOrNull()

    if (selectionMap == null) {
        logger.error { "Failed to convert CiphertextTallyContest's selection map" }
        return null
    }

    return CiphertextTally.Contest(this.contestId, this.sequenceOrder, contestHash, selectionMap)
}

private fun electionguard.protogen.CiphertextTallySelection.importSelection(
    groupContext: GroupContext
): CiphertextTally.Selection? {

    val selectionDescriptionHash = groupContext.importUInt256(this.selectionDescriptionHash)
    val ciphertext = groupContext.importCiphertext(this.ciphertext)

    if (selectionDescriptionHash == null || ciphertext == null) {
        logger.error { "Selection description hash or ciphertext was malformed or out of bounds" }
        return null
    }

    return CiphertextTally.Selection(
        this.selectionId,
        this.sequenceOrder,
        selectionDescriptionHash,
        ciphertext
    )
}

/////////////////////////////////////////////////////////////////////////////////////////////////

fun CiphertextTally.publishCiphertextTally(): electionguard.protogen.CiphertextTally {
    return electionguard.protogen
        .CiphertextTally(this.tallyId, this.contests.values.map { it.publishContest() })
}

private fun CiphertextTally.Contest.publishContest():
    electionguard.protogen.CiphertextTallyContest {
        return electionguard.protogen
            .CiphertextTallyContest(
                this.contestId,
                this.sequenceOrder,
                this.contestDescriptionHash.publishUInt256(),
                this.selections.values.map { it.publishSelection() }
            )
    }

private fun CiphertextTally.Selection.publishSelection():
    electionguard.protogen.CiphertextTallySelection {
        return electionguard.protogen
            .CiphertextTallySelection(
                this.selectionId,
                this.sequenceOrder,
                this.selectionDescriptionHash.publishUInt256(),
                this.ciphertext.publishCiphertext()
            )
    }