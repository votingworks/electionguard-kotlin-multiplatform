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
        for (entry in roundtrip.contests) {
            val contest =
                tally.contests.get(entry.key)
                    ?: throw RuntimeException("Cant find contest $entry.key")
            val rcontest = entry.value
            for (entry2 in rcontest.selections) {
                val selection = contest.selections.get(entry2.key)
                        ?: throw RuntimeException("Cant find selection $entry2.key")
                val rselection = entry2.value
                assertEquals(selection, rselection)
            }
        }
        assertEquals(roundtrip, tally)
    }

    companion object {

        fun generateFakeTally(seq: Int, context: GroupContext): DecryptedTallyOrBallot {
            val contests = List(7) { generateFakeContest(it, context) }
            return DecryptedTallyOrBallot("tallyId$seq", contests.associate { it.contestId to it })
        }

        private fun generateFakeContest(cseq: Int, context: GroupContext): DecryptedTallyOrBallot.Contest {
            val selections = List(11) { generateFakeSelection(it, context) }
            return DecryptedTallyOrBallot.Contest(
                "contest$cseq",
                selections.associate { it.selectionId to it },
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