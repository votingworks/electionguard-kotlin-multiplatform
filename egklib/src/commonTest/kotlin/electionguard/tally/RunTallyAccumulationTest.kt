package electionguard.tally

import kotlin.test.Test

class RunTallyAccumulationTest {

    @Test
    fun runTallyAccumulationTest() {
        main(
            arrayOf(
                "-in",
                "testOut/testRunBatchEncryptionTest",
                // "src/commonTest/data/runWorkflowAllAvailable",
                "-out",
                "testOut/runTallyAccumulationTest",
                "-name",
                "CountyCook-precinct079-device24358"
            )
        )
    }
}