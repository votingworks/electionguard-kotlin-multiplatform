package electionguard.tally

import kotlin.test.Test

class RunTallyAccumulationTest {

    @Test
    fun runTallyAccumulationTest() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/workflow/runBatchEncryption",
                "-out",
                "src/commonTest/data/workflow/runTallyAccumulation",
                "-name",
                "CountyCook-precinct079-device24358"
            )
        )
    }
}