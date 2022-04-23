package electionguard.tally

import kotlin.test.Test

class RunTallyAccumulationTest {

    @Test
    fun runTallyAccumulationTest() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/workflow",
                "-out",
                "src/commonTest/data/workflow",
                "-name",
                "CountyCook-precinct079-device24358"
            )
        )
    }
}