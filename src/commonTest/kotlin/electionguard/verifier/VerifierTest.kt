package electionguard.verifier

import electionguard.core.productionGroup
import kotlin.test.Test

class VerifierTest {
    @Test
    fun readElectionRecordAndValidate() {
        runVerifier(productionGroup(), "src/commonTest/data/runWorkflowAllAvailable", 11)
    }

    @Test
    fun readRecoveredElectionRecordAndValidate() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/runWorkflowSomeAvailable",
                "-nthreads",
                "1",
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