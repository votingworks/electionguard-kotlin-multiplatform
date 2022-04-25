package electionguard.tally

import kotlin.test.Test

class RunTallyAccumulationTest {

    @Test
    fun runTallyAccumulationTest() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/runWorkflow",
                "-out",
                "testOut/runTallyAccumulationTest",
                "-name",
                "CountyCook-precinct079-device24358"
            )
        )
    }
}