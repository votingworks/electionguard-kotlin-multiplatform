package electionguard.protoconvert

import electionguard.ballot.PlaintextBallot
import electionguard.core.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaintextBallotConvertTest {

    @Test
    fun roundtripPlaintextBallot() {
        runTest {
            val ballot = generateFakeBallot();
            val ballotConvert = PlaintextBallotConvert();
            val proto = ballotConvert.translateToProto(ballot)
            val roundtrip = ballotConvert.translateFromProto(proto)
            assertEquals(roundtrip, ballot)
        }
    }

    private fun generateFakeBallot(): PlaintextBallot {
        val contests = List(11, { generateFakeContest(it) })
        return PlaintextBallot("ballotId", "ballotIdStyle", contests)
    }

    private fun generateFakeContest(cseq: Int): PlaintextBallot.Contest {
        val selections = List(11, { generateFakeSelection(it) })
        return PlaintextBallot.Contest("contest" + cseq, cseq, selections)
    }

    private fun generateFakeSelection(sseq: Int): PlaintextBallot.Selection {
        val vote: Int = if (Random.nextBoolean()) 1 else 0
        return PlaintextBallot.Selection("selection" + sseq, sseq, vote, false, generateExtendedData(sseq))
    }

    private fun generateExtendedData(sseq: Int): PlaintextBallot.ExtendedData {
        return PlaintextBallot.ExtendedData("ExtendedData" + sseq, sseq)

    }
}