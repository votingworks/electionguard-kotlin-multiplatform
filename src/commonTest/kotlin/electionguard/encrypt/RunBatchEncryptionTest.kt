package electionguard.encrypt

import kotlin.test.Test

class RunBatchEncryptionTest {

    @Test
    fun testRunBatchEncryptionTest() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/runWorkflowAllAvailable",
                "-ballots",
                "src/commonTest/data/runWorkflowAllAvailable/private_data/input",
                "-out",
                "testOut/testRunBatchEncryptionTest",
                "-invalidBallots",
                "testOut/testRunBatchEncryptionTest/invalid_ballots",
                "-fixedNonces",
                "-nthreads",
                "1",
            )
        )
    }

    @Test
    fun testRunChannelEncryptionTest() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/runWorkflowAllAvailable",
                "-ballots",
                "src/commonTest/data/runWorkflowAllAvailable/private_data/input",
                "-out",
                "testOut/testRunBatchEncryptionTest",
                "-invalidBallots",
                "testOut/testRunBatchEncryptionTest/invalid_ballots",
                "-fixedNonces",
                "-nthreads",
                "11",
            )
        )
    }
}