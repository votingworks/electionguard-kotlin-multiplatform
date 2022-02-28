package electionguard.protoconvert

import electionguard.ballot.CiphertextTally
import electionguard.core.GroupContext
import electionguard.core.noNullValuesOrNull
import electionguard.protogen.CiphertextTallyContest
import electionguard.protogen.CiphertextTallySelection
import mu.KotlinLogging
private val logger = KotlinLogging.logger("CommonConvert")

data class CiphertextTallyConvert(val groupContext: GroupContext) {

    fun translateFromProto(proto: electionguard.protogen.CiphertextTally?): CiphertextTally? {
        if (proto == null) {
            return null
        }

        val contestMap = proto.contests.associate { it.contestId to convertContest(it) }.noNullValuesOrNull()

        if (contestMap == null) {
            logger.error { "Failed to convert CiphertextTally's contest map" }
            return null
        }

        return CiphertextTally(proto.tallyId, contestMap)
    }

    private fun convertContest(proto: CiphertextTallyContest?): CiphertextTally.Contest? {
        if (proto == null) {
            return null
        }

        val contestHash = convertElementModQ(proto.contestDescriptionHash, groupContext)

        if (contestHash == null) {
            logger.error { "Contest description hash was malformed or out of bounds" }
            return null
        }

        val selectionMap = proto.selections.associate { it.selectionId to convertSelection(it) }.noNullValuesOrNull()

        if (selectionMap == null) {
            logger.error { "Failed to convert CiphertextTallyContest's selection map" }
            return null
        }

        return CiphertextTally.Contest(proto.contestId, proto.sequenceOrder, contestHash, selectionMap)
    }

    private fun convertSelection(proto: CiphertextTallySelection?): CiphertextTally.Selection? {
        if (proto == null) {
            return null
        }

        val selectionDescriptionHash = convertElementModQ(proto.selectionDescriptionHash, groupContext)
        val ciphertext = convertCiphertext(proto.ciphertext, groupContext)

        if (selectionDescriptionHash == null || ciphertext == null) {
            logger.error { "Selection description hash or ciphertext was malformed or out of bounds" }
            return null
        }

        return CiphertextTally.Selection(proto.selectionId, proto.sequenceOrder, selectionDescriptionHash, ciphertext)
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