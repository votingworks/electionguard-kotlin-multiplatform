package electionguard.tally

import kotlin.test.Test

class RunTallyAccumulationTest {

    @Test
    fun runTallyAccumulationTest() {
        main(
            arrayOf(
                "-in",
                "/home/snake/tmp/electionguard/kotlin/runBatchEncryption",
                "-out",
                "/home/snake/tmp/electionguard/kotlin/runTallyAccumulation",
                "-name",
                "CountyCook-precinct079-device24358"
            )
        )
    }
}