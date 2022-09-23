package electionguard.verifier

import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.publish.Consumer
import electionguard.publish.electionRecordFromConsumer
import kotlin.test.Test
import kotlin.test.assertTrue

// run jvm verifier on native election record
class VerifyNativeRecordTest {
    val nativeDir = "src/commonTest/data/testElectionRecord/native/"

    // @Test
    fun verifyNativeRecordTest() {
        runTest {
            val group = productionGroup()
            val electionRecord = electionRecordFromConsumer(Consumer(nativeDir, group))
            val verifier = Verifier(electionRecord)
            val ok = verifier.verify()
            assertTrue(ok)
        }
    }

}