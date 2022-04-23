package electionguard.protoconvert

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.ballot.CiphertextTally
import electionguard.core.GroupContext
import electionguard.core.tinyGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CiphertextTallyConvertTest {

    @Test
    fun roundtripCiphertextTally() {
        val context = tinyGroup()
        val tally = generateFakeTally(context)
        val proto: electionguard.protogen.CiphertextTally = tally.publishCiphertextTally()
        val roundtrip = context.importCiphertextTally(proto)
        assertTrue(roundtrip is Ok)
        assertEquals(roundtrip.unwrap(), tally)
    }

    companion object {
        fun generateFakeTally(context: GroupContext): CiphertextTally {
            val contests = List(7) { generateFakeContest(it, context) }
            return CiphertextTally("tallyId", contests.associate { it.contestId to it })
        }

        private fun generateFakeContest(cseq: Int, context: GroupContext): CiphertextTally.Contest {
            val selections = List(11) { generateFakeSelection(it, context) }
            return CiphertextTally.Contest(
                "contest" + cseq,
                cseq,
                generateUInt256(context),
                selections.associate { it.selectionId to it },
            )
        }

        private fun generateFakeSelection(
            sseq: Int,
            context: GroupContext
        ): CiphertextTally.Selection {
            //         val selectionId: String,
            //        val sequenceOrder: Int,
            //        val selectionDescriptionHash: ElementModQ,
            //        val ciphertext: ElGamalCiphertext,
            return CiphertextTally.Selection(
                "selection" + sseq,
                sseq,
                generateUInt256(context),
                generateCiphertext(context),
            )
        }
    }
}