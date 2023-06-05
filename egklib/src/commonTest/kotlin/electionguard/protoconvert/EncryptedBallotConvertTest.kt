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
        val context = productionGroup()
        val ballot = generateEncryptedBallot(42, context)
        val proto = ballot.publishProto()
        val roundtrip = proto.import(context)
        assertTrue(roundtrip is Ok)
        assertEquals(roundtrip.unwrap(), ballot)
    }

    @Test
    fun roundtripEncryptedBallotTiny() {
        val context = tinyGroup()
        val ballot = generateEncryptedBallot(42, context)
        val proto = ballot.publishProto()
        val roundtrip = proto.import(context)
        assertTrue(roundtrip is Ok)
        assertEquals(roundtrip.unwrap(), ballot)
    }

    fun generateEncryptedBallot(seq: Int, context: GroupContext): EncryptedBallot {
        val contests = List(9, { generateFakeContest(it, context) })
        return EncryptedBallot(
            "ballotId $seq",
            "ballotIdStyle",
            generateUInt256(context),
            ByteArray(0),
            contests,
            42,
            if (Random.nextBoolean())
                EncryptedBallot.BallotState.CAST
            else
                EncryptedBallot.BallotState.SPOILED,
        )
    }

    private fun generateFakeContest(cseq: Int, context: GroupContext): EncryptedBallot.Contest {
        val selections = List(11, { generateFakeSelection(it, context) })
        return EncryptedBallot.Contest(
            "contest" + cseq,
            cseq,
            generateUInt256(context),
            selections,
            generateRangeChaumPedersenProofKnownNonce(context),
            generateHashedCiphertext(context),
        )
    }

    private fun generateFakeSelection(
        sseq: Int,
        context: GroupContext
    ): EncryptedBallot.Selection {
        return EncryptedBallot.Selection(
            "selection" + sseq,
            sseq,
            generateCiphertext(context),
            generateRangeChaumPedersenProofKnownNonce(context),
        )
    }

    @Test
    fun unknownBallotState() {
        val context = productionGroup()
        val ballot = generateEncryptedBallot(42, context).copy(state = EncryptedBallot.BallotState.UNKNOWN)
        val proto = ballot.publishProto()
        val roundtrip = proto.import(context)
        assertTrue(roundtrip is Ok)
        assertEquals(roundtrip.unwrap(), ballot)
    }
}