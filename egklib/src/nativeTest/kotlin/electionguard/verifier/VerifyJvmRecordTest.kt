package electionguard.verifier

import electionguard.core.Stats
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.publish.ConsumerProto
import electionguard.publish.electionRecordFromConsumer
import kotlin.test.Test
import kotlin.test.assertTrue

// run native verifier on jvm election record
class VerifyJvmRecordTest {
    val jvmDir = "src/commonTest/data/runWorkflowSomeAvailable"

    @Test
    fun verifyNativeRecordTest() {
        runTest {
            val group = productionGroup()
            val electionRecord = electionRecordFromConsumer(ConsumerProto(jvmDir, group))
            val verifier = Verifier(electionRecord)
            val ok = verifier.verify(Stats())
            assertTrue(ok)
        }
    }

}