package electionguard.encrypt

import kotlin.test.Test

class RunBatchEncryptionTest {

    @Test
    fun testRunBatchEncryptionNonces() {
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
    fun testRunBatchEncryption() {
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
                "1",
            )
        )
    }

    @Test
    fun testRunBatchEncryptionEncryptTwice() {
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
                "1",
                "-check",
                "EncryptTwice"
            )
        )
    }

    @Test
    fun testRunBatchEncryptionVerify() {
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
                "1",
                "-check",
                "Verify",
            )
        )
    }

}