package electionguard.protoconvert

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.tinyGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TallyResultConvertTest {

    @Test
    fun roundtripTallyResult() {
        val context = tinyGroup()
        val electionRecord = generateTallyResult(context)
        val proto = electionRecord.publishProto()
        val roundtrip = proto.import(context).getOrThrow { IllegalStateException(it) }
        assertNotNull(roundtrip)

        assertEquals(roundtrip.electionInitialized, electionRecord.electionInitialized)
        assertEquals(roundtrip.encryptedTally, electionRecord.encryptedTally)
        assertEquals(roundtrip.tallyIds, electionRecord.tallyIds)
        assertEquals(roundtrip, electionRecord)
    }
}

fun generateTallyResult(context: GroupContext): TallyResult {
    return TallyResult(
        generateElectionInitialized(context),
        EncryptedTallyConvertTest.generateFakeTally(context),
        listOf("ballotID1", "ballotsId42"),
        listOf("precinct342342", "precinct3423333"),
    )
}