package electionguard.verifier

import electionguard.core.productionGroup
import kotlin.test.Test

class VerifierTest {
    @Test
    fun readElectionRecordAndValidate() {
        runVerifier(productionGroup(), "src/commonTest/data/runWorkflowAllAvailable", 11, true)
    }

    @Test
    fun readRecoveredElectionRecordAndValidate() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/runWorkflowSomeAvailable",
                "-nthreads",
                "11",
                "--showTime",
                "--help",
            )
        )
    }

    @Test
    fun testVerifyEncryptedBallots() {
        verifyEncryptedBallots(productionGroup(), "src/commonTest/data/runWorkflowSomeAvailable", 11)
    }

    @Test
    fun verifyDecryptedTallyWithRecoveredShares() {
        verifyDecryptedTally(productionGroup(), "src/commonTest/data/runWorkflowSomeAvailable")
    }

    @Test
    fun verifyRecoveredShareResults() {
        verifyRecoveredShares(productionGroup(), "src/commonTest/data/runWorkflowSomeAvailable")
    }

    @Test
    fun verifySpoiledBallotTallies() {
        verifySpoiledBallotTallies(productionGroup(), "src/commonTest/data/runWorkflowSomeAvailable")
    }
}