package electionguard.protoconvert

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.core.GroupContext
import electionguard.core.tinyGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DecryptedTallyOrBallotConvertTest {

    @Test
    fun roundtripDecryptedTallyOrBallot() {
        val context = tinyGroup()
        val tally = generateFakeTally(1, context)
        val proto = tally.publishDecryptedTallyOrBallot()
        val roundtrip = context.importDecryptedTallyOrBallot(proto).getOrThrow { IllegalStateException(it) }
        assertNotNull(roundtrip)
        val tallyContestMap = tally.contests.associateBy { it.contestId }
        for (rtContest in roundtrip.contests) {
            val contest = tallyContestMap.get(rtContest.contestId)
                    ?: throw RuntimeException("Cant find contest ${rtContest.contestId}")
            val tallySelectionMap = contest.selections.associateBy { it.selectionId }
            for (rtSelection in rtContest.selections) {
                val selection = tallySelectionMap.get(rtSelection.selectionId)
                        ?: throw RuntimeException("Cant find selection ${rtSelection.selectionId}")
                assertEquals(selection, rtSelection)
            }
        }
        assertEquals(roundtrip, tally)
    }

    companion object {

        fun generateFakeTally(seq: Int, context: GroupContext): DecryptedTallyOrBallot {
            val contests = List(7) { generateFakeContest(it, context) }
            return DecryptedTallyOrBallot("tallyId$seq", contests)
        }

        private fun generateFakeContest(cseq: Int, context: GroupContext): DecryptedTallyOrBallot.Contest {
            val selections = List(11) { generateFakeSelection(it, context) }
            return DecryptedTallyOrBallot.Contest(
                "contest$cseq",
                selections,
                null, // TODO
            )
        }

        private fun generateFakeSelection(
            sseq: Int,
            context: GroupContext
        ): DecryptedTallyOrBallot.Selection {
            return DecryptedTallyOrBallot.Selection(
                "selection$sseq",
                sseq,
                generateElementModP(context),
                generateCiphertext(context),
                generateGenericChaumPedersenProof(context)
            )
        }
    }
}