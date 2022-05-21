package electionguard.protoconvert

import com.github.michaelbull.result.*
import electionguard.ballot.EncryptedBallot
import electionguard.core.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EncryptedBallotConvertTest {

    @Test
    fun roundtripEncryptedBallot() {
        val context = tinyGroup()
        val ballot = generateEncryptedBallot(42, context)
        val proto = ballot.publishEncryptedBallot()
        val roundtrip = context.importEncryptedBallot(proto)
        assertTrue(roundtrip is Ok)
        assertEquals(roundtrip.unwrap(), ballot)
    }

    companion object {
        fun generateEncryptedBallot(seq: Int, context: GroupContext): EncryptedBallot {
            val contests = List(9, { generateFakeContest(it, context) })
            //     val ballotId: String,
            //    val ballotStyleId: String,
            //    val manifestHash: ElementModQ,
            //    val previousTrackingHash: ElementModQ,
            //    val trackingHash: ElementModQ,
            //    val contests: List<Contest>,
            //    val timestamp: Long,
            //    val cryptoHash: ElementModQ,
            //    val state: BallotState,
            return EncryptedBallot(
                "ballotId $seq",
                "ballotIdStyle",
                generateUInt256(context),
                generateUInt256(context),
                generateUInt256(context),
                contests,
                42,
                generateUInt256(context),
                if (Random.nextBoolean())
                    EncryptedBallot.BallotState.CAST
                else
                    EncryptedBallot.BallotState.SPOILED,
            )
        }

        private fun generateFakeContest(cseq: Int, context: GroupContext): EncryptedBallot.Contest {
            val selections = List(11, { generateFakeSelection(it, context) })
            //         val contestId: String,
            //        val sequenceOrder: Int,
            //        val contestHash: ElementModQ,
            //        val selections: List<Selection>,
            //        val ciphertextAccumulation: ElGamalCiphertext,
            //        val cryptoHash: ElementModQ,
            //        val proof: ConstantChaumPedersenProofKnownNonce?,
            return EncryptedBallot.Contest(
                "contest" + cseq,
                cseq,
                generateUInt256(context),
                selections,
                generateUInt256(context),
                generateConstantChaumPedersenProofKnownNonce(context),
            )
        }

        /* val selectionId: String,
         *        val sequenceOrder: Int,
         *        val selectionHash: ElementModQ,
         *        val ciphertext: ElGamalCiphertext,
         *        val cryptoHash: ElementModQ,
         *        val isPlaceholderSelection: Boolean,
         *        val proof: DisjunctiveChaumPedersenProofKnownNonce?,
         *        val extendedData: ElGamalCiphertext?, */
        private fun generateFakeSelection(
            sseq: Int,
            context: GroupContext
        ): EncryptedBallot.Selection {
            return EncryptedBallot.Selection(
                "selection" + sseq,
                sseq,
                generateUInt256(context),
                generateCiphertext(context),
                generateUInt256(context),
                false,
                generateDisjunctiveChaumPedersenProofKnownNonce(context),
                generateHashedCiphertext(context),
            )
        }
    }
}