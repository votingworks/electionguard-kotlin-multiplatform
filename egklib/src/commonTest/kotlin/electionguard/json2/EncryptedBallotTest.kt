package electionguard.json2

import electionguard.ballot.EncryptedBallot
import electionguard.core.*
import electionguard.util.ErrorMessages
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EncryptedBallotTest {

    @Test
    fun roundtripEncryptedBallot() {
        val context = productionGroup()
        val ballot = generateEncryptedBallot(42, context)
        val json = ballot.publishJson()
        val roundtrip = json.import(context, ErrorMessages(""))
        assertNotNull(roundtrip)
        assertEquals(ballot, roundtrip)
        assertTrue("baux".encodeToByteArray().contentEquals(roundtrip.codeBaux))
    }

    @Test
    fun roundtripEncryptedBallotTiny() {
        val context = tinyGroup()
        val ballot = generateEncryptedBallot(42, context)
        val json = ballot.publishJson()
        val roundtrip = json.import(context, ErrorMessages(""))
        assertNotNull(roundtrip)
        assertEquals(ballot, roundtrip)
    }

    fun generateEncryptedBallot(seq: Int, context: GroupContext): EncryptedBallot {
        val contests = List(9, { generateFakeContest(it, context) })
        return EncryptedBallot(
            "ballotId $seq",
            "ballotIdStyle",
            "device",
            42,
            "baux".encodeToByteArray(),
            generateUInt256(context),
            generateUInt256(context),
            contests,
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
            1,
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
        val json = ballot.publishJson()
        val roundtrip = json.import(context, ErrorMessages(""))
        assertNotNull(roundtrip)
        assertEquals(ballot, roundtrip)
    }
}