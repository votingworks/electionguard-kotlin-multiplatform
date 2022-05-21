package electionguard.protoconvert

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.tinyGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TallyResultConvertTest {

    @Test
    fun roundtripTallyResult() {
        val context = tinyGroup()
        val electionRecord = generateTallyResult(context)
        val proto = electionRecord.publishTallyResult()
        val roundtrip = context.importTallyResult(proto).getOrThrow { IllegalStateException(it) }
        assertNotNull(roundtrip)

        assertEquals(roundtrip.group, electionRecord.group)
        assertEquals(roundtrip.electionIntialized, electionRecord.electionIntialized)
        assertEquals(roundtrip.encryptedTally, electionRecord.encryptedTally)
        assertEquals(roundtrip.tallyIds, electionRecord.tallyIds)

        assertTrue(roundtrip.equals(electionRecord))
        assertEquals(roundtrip, electionRecord)
    }
}

fun generateTallyResult(context: GroupContext): TallyResult {
    return TallyResult(
        context,
        generateElectionInitialized(context),
        EncryptedTallyConvertTest.generateFakeTally(context),
        listOf("ballotID1", "ballotsId42"),
        listOf("precinct342342", "precinct3423333"),
    )
}