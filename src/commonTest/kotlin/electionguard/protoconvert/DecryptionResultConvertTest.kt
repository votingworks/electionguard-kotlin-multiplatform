package electionguard.protoconvert

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.tinyGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DecryptionResultConvertTest {

    @Test
    fun roundtripDecryptionResult() {
        val context = tinyGroup()
        val electionRecord = generateDecryptionResult(context)
        val proto = electionRecord.publishDecryptionResult()
        val roundtrip = context.importDecryptionResult(proto).getOrThrow { IllegalStateException(it) }
        assertNotNull(roundtrip)

        assertEquals(roundtrip.tallyResult, electionRecord.tallyResult)
        assertEquals(roundtrip.decryptedTallyOrBallot, electionRecord.decryptedTallyOrBallot)
        assertEquals(roundtrip.lagrangeCoordinates, electionRecord.lagrangeCoordinates)
        assertEquals(roundtrip.metadata, electionRecord.metadata)

        assertTrue(roundtrip.equals(electionRecord))
        assertEquals(roundtrip, electionRecord)
    }
}

fun generateDecryptionResult(context: GroupContext): DecryptionResult {
    return DecryptionResult(
        generateTallyResult(context),
        DecryptedTallyOrBallotConvertTest.generateFakeTally(0, context),
        List(4) { generateDecryptingGuardian(context, it) },
    )
}

fun generateDecryptingGuardian(context: GroupContext, seq: Int): LagrangeCoordinate {
    return LagrangeCoordinate("aguardian $seq", seq + 1, generateElementModQ(context))
}