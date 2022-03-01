package electionguard.protoconvert

import electionguard.ballot.CiphertextTally
import electionguard.core.GroupContext
import electionguard.core.tinyGroup
import kotlin.test.Test
import kotlin.test.assertEquals

class CiphertextTallyConvertTest {

    @Test
    fun roundtripCiphertextTally() {
        val context = tinyGroup()
        val tally = generateFakeTally(context)
        val proto = tally.publishCiphertextTally()
        val roundtrip = proto.importCiphertextTally(context)
        assertEquals(roundtrip, tally)
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
                generateElementModQ(context),
                selections.associate { it.selectionId to it },
            )
        }

        private fun generateFakeSelection(sseq: Int, context: GroupContext): CiphertextTally.Selection {
            //         val selectionId: String,
            //        val sequenceOrder: Int,
            //        val selectionDescriptionHash: ElementModQ,
            //        val ciphertext: ElGamalCiphertext,
            return CiphertextTally.Selection(
                "selection" + sseq,
                sseq,
                generateElementModQ(context),
                generateCiphertext(context),
            )
        }
    }

}