@file:OptIn(ExperimentalCli::class)

package electionguard.publish

import electionguard.core.productionGroup

import kotlinx.cli.ExperimentalCli
import kotlin.test.Test

/** Test DecryptingMediator with in-process DecryptingTrustee's. Cannot use this in production */
class RunConvertVer1Test {

    @Test
    fun testConvertVer1() {
        val group = productionGroup()
        val inputDir =  "src/commonTest/data/testJava/kickstart/encryptor/election_record/electionRecord.protobuf"
        val outputDir =  "/home/snake/tmp/electionguard/kotlin2"
        val electionInit = group.readElectionRecordVer1(inputDir)

        val publisher = ElectionRecord(outputDir, group, PublisherMode.createIfMissing)
        publisher.writeElectionInitialized(electionInit)
    }
}
