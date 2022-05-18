package electionguard.encrypt

import kotlin.test.Test

class RunBatchEncryptionTest {

    @Test
    fun testRunBatchEncryptionNoncesTest() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/runWorkflowAllAvailable",
                "-ballots",
                "src/commonTest/data/runWorkflowAllAvailable/private_data/input",
                "-out",
                "testOut/testRunBatchEncryptionNoncesTest",
                "-invalid",
                "testOut/testRunBatchEncryptionNoncesTest/invalid_ballots",
                "-fixed",
                "-nthreads",
                "1",
            )
        )
    }

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
                "-invalid",
                "testOut/testRunBatchEncryptionTest/invalid_ballots",
                "-nthreads",
                "12",
            )
        )
    }
}