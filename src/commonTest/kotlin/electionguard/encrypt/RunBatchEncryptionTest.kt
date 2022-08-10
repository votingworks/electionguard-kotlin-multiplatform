package electionguard.encrypt

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.ElectionInitialized
import electionguard.core.productionGroup
import electionguard.input.RandomBallotProvider
import electionguard.publish.Consumer
import kotlin.test.Test
import kotlin.test.assertContains

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
                "11",
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

    @Test
    fun testInvalidBallot() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/runWorkflowAllAvailable"
        val invalidDir = "testOut/testInvalidBallot/invalid_ballots"
        val consumerIn = Consumer(inputDir, group)
        val electionInit: ElectionInitialized =
            consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }
        val ballots = RandomBallotProvider(electionInit.manifest(), 1).ballots("badStyleId")

        batchEncryption(
            group,
            "src/commonTest/data/runWorkflowAllAvailable",
            "testOut/testInvalidBallot",
            ballots,
            invalidDir,
            false,
            1,
            "testInvalidBallot",
        )

        val consumerOut = Consumer(invalidDir, group)
        consumerOut.iteratePlaintextBallots(invalidDir, null).forEach {
            println("${it.errors}")
            assertContains(it.errors.toString(), "Ballot.A.1 Ballot Style 'badStyleId' does not exist in election")
        }

    }

}