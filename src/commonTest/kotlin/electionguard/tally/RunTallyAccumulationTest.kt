package electionguard.tally

import kotlin.test.Test

class RunTallyAccumulationTest {

    @Test
    fun runTallyAccumulationTest() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/native/runBatchEncryption",
                "-out",
                "src/commonTest/data/native/runTallyAccumulation",
                "-name",
                "CountyCook-precinct079-device24358"
            )
        )
    }
}