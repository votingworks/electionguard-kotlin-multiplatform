package electionguard.tally

import kotlin.test.Test

class RunTallyAccumulationTest {

    @Test
    fun runTallyAccumulationTestJson() {
        main(
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
        main(
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