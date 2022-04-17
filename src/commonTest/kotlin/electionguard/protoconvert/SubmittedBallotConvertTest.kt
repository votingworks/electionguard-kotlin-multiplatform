package electionguard.protoconvert

import com.github.michaelbull.result.*
import electionguard.ballot.SubmittedBallot
import electionguard.core.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubmittedBallotConvertTest {

    @Test
    fun roundtripSubmittedBallot() {
        val context = tinyGroup()
        val ballot = generateSubmittedBallot(42, context)
        val proto = ballot.publishSubmittedBallot()
        val roundtrip = context.importSubmittedBallot(proto)
        assertTrue(roundtrip is Ok)
        assertEquals(roundtrip.unwrap(), ballot)
    }

    companion object {
        fun generateSubmittedBallot(seq: Int, context: GroupContext): SubmittedBallot {
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
            return SubmittedBallot(
                "ballotId $seq",
                "ballotIdStyle",
                generateUInt256(context),
                generateUInt256(context),
                generateUInt256(context),
                contests,
                42,
                generateUInt256(context),
                if (Random.nextBoolean())
                    SubmittedBallot.BallotState.CAST
                else
                    SubmittedBallot.BallotState.SPOILED,
            )
        }

        private fun generateFakeContest(cseq: Int, context: GroupContext): SubmittedBallot.Contest {
            val selections = List(11, { generateFakeSelection(it, context) })
            //         val contestId: String,
            //        val sequenceOrder: Int,
            //        val contestHash: ElementModQ,
            //        val selections: List<Selection>,
            //        val ciphertextAccumulation: ElGamalCiphertext,
            //        val cryptoHash: ElementModQ,
            //        val proof: ConstantChaumPedersenProofKnownNonce?,
            return SubmittedBallot.Contest(
                "contest" + cseq,
                cseq,
                generateUInt256(context),
                selections,
                generateCiphertext(context),
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
        ): SubmittedBallot.Selection {
            return SubmittedBallot.Selection(
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