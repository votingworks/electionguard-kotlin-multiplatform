package electionguard.verifier

import electionguard.core.Stats
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.publish.readElectionRecord
import kotlin.test.assertTrue

// run jvm verifier on native election record
class VerifyNativeRecordTest {
    val nativeDir = "src/commonTest/data/testElectionRecord/native/"

    // @Test TODO
    fun verifyNativeRecordTest() {
        runTest {
            val group = productionGroup()
            val electionRecord = readElectionRecord(group, nativeDir)
            val verifier = Verifier(electionRecord)
            val stats = Stats()
            val ok = verifier.verify(stats)
            stats.show()
            assertTrue(ok)
        }
    }

}