package electionguard.protoconvert

import electionguard.ballot.EncryptedTally
import electionguard.core.*
import electionguard.util.ErrorMessages
import org.junit.jupiter.api.Assertions.assertNotNull
import kotlin.test.Test
import kotlin.test.assertEquals

class EncryptedTallyConvertTest {

    @Test
    fun roundtripCiphertextTally() {
        val context = tinyGroup()
        val tally = generateFakeTally(context)
        val proto: electionguard.protogen.EncryptedTally = tally.publishProto()
        val roundtrip = proto.import(context, ErrorMessages(""))
        assertNotNull(roundtrip)
        assertEquals(roundtrip, tally)
    }

    companion object {
        fun generateFakeTally(context: GroupContext): EncryptedTally {
            val contests = List(7) { generateFakeContest(it, context) }
            return EncryptedTally("tallyId", contests, listOf("device1"), UInt256.random(),)
        }

        private fun generateFakeContest(cseq: Int, context: GroupContext): EncryptedTally.Contest {
            val selections = List(11) { generateFakeSelection(it, context) }
            return EncryptedTally.Contest(
                "contest$cseq",
                cseq,
                selections,
            )
        }

        private fun generateFakeSelection(
            sseq: Int,
            context: GroupContext
        ): EncryptedTally.Selection {
            return EncryptedTally.Selection(
                "selection$sseq",
                sseq,
                generateCiphertext(context),
            )
        }
    }
}