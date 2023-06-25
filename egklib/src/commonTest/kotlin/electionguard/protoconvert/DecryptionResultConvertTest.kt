package electionguard.protoconvert

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.tinyGroup
import electionguard.publish.Publisher
import electionguard.publish.makePublisher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DecryptionResultConvertTest {
    val outputDir = "testOut/protoconvert/DecryptionResultConvertTest"
    val publisher = makePublisher(outputDir, true)

    @Test
    fun roundtripDecryptionResult() {
        val context = tinyGroup()
        val electionRecord = generateDecryptionResult(publisher, context)
        val proto = electionRecord.publishProto()
        val roundtrip = proto.import(context).getOrThrow { IllegalStateException(it) }
        assertNotNull(roundtrip)

        assertEquals(roundtrip.tallyResult, electionRecord.tallyResult)
        assertEquals(roundtrip.decryptedTally, electionRecord.decryptedTally)
        // assertEquals(roundtrip.lagrangeCoordinates, electionRecord.lagrangeCoordinates)
        assertEquals(roundtrip.metadata, electionRecord.metadata)

        assertTrue(roundtrip.equals(electionRecord))
        assertEquals(roundtrip, electionRecord)
    }
}

fun generateDecryptionResult(publisher : Publisher, context: GroupContext): DecryptionResult {
    return DecryptionResult(
        generateTallyResult(publisher, context),
        DecryptedTallyOrBallotConvertTest.generateFakeTally(0, context),
        // List(4) { generateDecryptingGuardian(context, it) },
    )
}