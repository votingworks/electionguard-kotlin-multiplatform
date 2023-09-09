package electionguard.tally

import electionguard.cli.RunAccumulateTally
import kotlin.test.Test

class RunTallyAccumulationTest {

    @Test
    fun runTallyAccumulationTestJson() {
        RunAccumulateTally.main(
            arrayOf(
                "-in",
                "src/commonTest/data/workflow/someAvailableJson",
                "-out",
                "testOut/tally/testRunBatchEncryptionJson",
            )
        )
    }

    @Test
    fun runTallyAccumulationTestProto() {
        RunAccumulateTally.main(
            arrayOf(
                "-in",
                "src/commonTest/data/workflow/someAvailableProto",
                "-out",
                "testOut/tally/runTallyAccumulationTestProto",
                "-name",
                "CountyCook-precinct079-device24358",
                "-createdBy",
                "runTallyAccumulationTestProto",
            )
        )
    }
}