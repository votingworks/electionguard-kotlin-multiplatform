package electionguard.verifier

import electionguard.core.Stats
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.publish.readElectionRecord
import kotlin.test.Test
import kotlin.test.assertTrue

// run native verifier on jvm election record
class VerifyJvmRecordTest {
    val jvmDir = "src/commonTest/data/workflow/someAvailableProto"

    @Test
    fun verifyNativeRecordTest() {
        runTest {
            val group = productionGroup()
            val electionRecord = readElectionRecord(group, jvmDir)
            val verifier = Verifier(electionRecord)
            val ok = verifier.verify(Stats())
            assertTrue(ok)
        }
    }

}