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
        val proto = ballot.publishEncryptedBallot()
        val roundtrip = context.importEncryptedBallot(proto)
        assertTrue(roundtrip is Ok)
        assertEquals(roundtrip.unwrap(), ballot)
    }

    @Test
    fun roundtripEncryptedBallotTiny() {
        val context = tinyGroup()
        val ballot = generateEncryptedBallot(42, context)
        val proto = ballot.publishEncryptedBallot()
        val roundtrip = context.importEncryptedBallot(proto)
        assertTrue(roundtrip is Ok)
        assertEquals(roundtrip.unwrap(), ballot)
    }

    fun generateEncryptedBallot(seq: Int, context: GroupContext): EncryptedBallot {
        val contests = List(9, { generateFakeContest(it, context) })
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
        return EncryptedBallot.Contest(
            "contest" + cseq,
            cseq,
            generateUInt256(context),
            selections,
            generateUInt256(context),
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
            generateUInt256(context),
            generateCiphertext(context),
            generateUInt256(context),
            generateRangeChaumPedersenProofKnownNonce(context),
        )
    }
}