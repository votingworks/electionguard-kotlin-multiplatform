package electionguard.publish

import electionguard.core.productionGroup
import kotlin.test.Test

// run native verifier on jvm election record
class ReadElectionRecordTest {
    val jvmDir = "src/commonTest/data/someAvailable"

    @Test
    fun readElectionRecordTest() {
        val group = productionGroup()
        val electionRecord = readElectionRecord(group, jvmDir)
    }

}