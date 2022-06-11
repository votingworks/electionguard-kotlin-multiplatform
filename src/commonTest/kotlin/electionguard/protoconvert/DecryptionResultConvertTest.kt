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

        //    val tallyResult: TallyResult,
        //    //    val decryptedTally: PlaintextTally,
        //    //    val availableGuardians: List<AvailableGuardian>,
        //    //    val metadata: Map<String, String> = emptyMap()
        assertEquals(roundtrip.tallyResult, electionRecord.tallyResult)
        assertEquals(roundtrip.decryptedTally, electionRecord.decryptedTally)
        assertEquals(roundtrip.decryptingGuardians, electionRecord.decryptingGuardians)
        assertEquals(roundtrip.metadata, electionRecord.metadata)

        assertTrue(roundtrip.equals(electionRecord))
        assertEquals(roundtrip, electionRecord)
    }
}

fun generateDecryptionResult(context: GroupContext): DecryptionResult {
    //      val tallyResult: TallyResult,
    //    val decryptedTally: PlaintextTally,
    //    val availableGuardians: List<AvailableGuardian>,
    //    val metadata: Map<String, String> = emptyMap()
    return DecryptionResult(
        generateTallyResult(context),
        PlaintextTallyConvertTest.generateFakeTally(0, context),
        List(4) { generateDecryptingGuardian(context, it) },
    )
}

fun generateDecryptingGuardian(context: GroupContext, seq: Int): DecryptingGuardian {
    return DecryptingGuardian("aguardian $seq", seq + 1, generateElementModQ(context))
}