package electionguard.protoconvert

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedTally
import electionguard.core.GroupContext
import electionguard.core.tinyGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EncryptedTallyConvertTest {

    @Test
    fun roundtripCiphertextTally() {
        val context = tinyGroup()
        val tally = generateFakeTally(context)
        val proto: electionguard.protogen.EncryptedTally = tally.publishEncryptedTally()
        val roundtrip = context.importEncryptedTally(proto)
        assertTrue(roundtrip is Ok)
        assertEquals(roundtrip.unwrap(), tally)
    }

    companion object {
        fun generateFakeTally(context: GroupContext): EncryptedTally {
            val contests = List(7) { generateFakeContest(it, context) }
            return EncryptedTally("tallyId", contests)
        }

        private fun generateFakeContest(cseq: Int, context: GroupContext): EncryptedTally.Contest {
            val selections = List(11) { generateFakeSelection(it, context) }
            return EncryptedTally.Contest(
                "contest$cseq",
                cseq,
                generateUInt256(context),
                selections,
            )
        }

        private fun generateFakeSelection(
            sseq: Int,
            context: GroupContext
        ): EncryptedTally.Selection {
            //         val selectionId: String,
            //        val sequenceOrder: Int,
            //        val selectionDescriptionHash: ElementModQ,
            //        val ciphertext: ElGamalCiphertext,
            return EncryptedTally.Selection(
                "selection$sseq",
                sseq,
                generateUInt256(context),
                generateCiphertext(context),
            )
        }
    }
}