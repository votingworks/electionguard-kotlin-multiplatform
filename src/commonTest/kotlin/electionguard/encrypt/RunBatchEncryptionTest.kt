package electionguard.encrypt

import kotlin.test.Test

class RunBatchEncryptionTest {

    @Test
    fun testRunBatchEncryptionTest() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/runWorkflow",
                "-ballots",
                "src/commonTest/data/runWorkflow/private_data/input",
                "-out",
                "testOut/testRunBatchEncryptionTest",
                "-invalidBallots",
                "testOut/testRunBatchEncryptionTest/invalid_ballots",
                "-fixedNonces",
                "-nthreads",
                "11"
            )
        )
    }
}