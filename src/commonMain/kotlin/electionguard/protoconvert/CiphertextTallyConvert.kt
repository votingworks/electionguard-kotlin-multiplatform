package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.ballot.CiphertextTally
import electionguard.core.GroupContext
import electionguard.core.noNullValuesOrNull
import mu.KotlinLogging

private val logger = KotlinLogging.logger("CommonConvert")

fun GroupContext.importCiphertextTally(tally: electionguard.protogen.CiphertextTally?):
        Result<CiphertextTally, String> {
    if (tally == null) {
        return Err("Null CiphertextTally")
    }
    val contestMap =
        tally.contests
            .associate { it.contestId to this.importContest(it) }
            .noNullValuesOrNull()

    if (contestMap == null) {
        return Err("Failed to convert CiphertextTally's contest map" )
    }

    return Ok(CiphertextTally(tally.tallyId, contestMap))
}

private fun GroupContext.importContest(contest: electionguard.protogen.CiphertextTallyContest): CiphertextTally.Contest? {

    val contestHash = importUInt256(contest.contestDescriptionHash)

    if (contestHash == null) {
        logger.error { "Contest description hash was malformed or out of bounds" }
        return null
    }

    val selectionMap =
        contest.selections
            .associate { it.selectionId to this.importSelection(it) }
            .noNullValuesOrNull()

    if (selectionMap == null) {
        logger.error { "Failed to convert CiphertextTallyContest's selection map" }
        return null
    }

    return CiphertextTally.Contest(contest.contestId, contest.sequenceOrder, contestHash, selectionMap)
}

private fun GroupContext.importSelection(selection: electionguard.protogen.CiphertextTallySelection): CiphertextTally.Selection? {

    val selectionDescriptionHash = importUInt256(selection.selectionDescriptionHash)
    val ciphertext = this.importCiphertext(selection.ciphertext)

    if (selectionDescriptionHash == null || ciphertext == null) {
        logger.error { "Selection description hash or ciphertext was malformed or out of bounds" }
        return null
    }

    return CiphertextTally.Selection(
        selection.selectionId,
        selection.sequenceOrder,
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