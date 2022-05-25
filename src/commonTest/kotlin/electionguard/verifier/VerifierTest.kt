package electionguard.verifier

import electionguard.core.productionGroup
import electionguard.core.runTest
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
            )
        )
    }

    @Test
    fun testVerifyEncryptedBallots() {
        verifyEncryptedBallots(productionGroup(), "src/commonTest/data/runWorkflowSomeAvailable", 1)
    }

}