package electionguard.tally

import kotlin.test.Test

class RunTallyAccumulationTest {

    @Test
    fun runTallyAccumulationTestJson() {
        main(
            arrayOf(
                "-in",
                "testOut/encrypt/testRunBatchEncryptionJson",
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
                "testOut/encrypt/testRunBatchEncryptionProto",
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