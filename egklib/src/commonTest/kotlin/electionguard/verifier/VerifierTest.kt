package electionguard.verifier

import electionguard.core.productionGroup
import kotlin.test.Test

class VerifierTest {
    @Test
    fun verification1() {
        runVerifier(productionGroup(), "src/commonTest/data/start", 1, true)
    }

    @Test
    fun verification2() {
        runVerifier(productionGroup(), "testOut/RunKeyCeremonyTest", 1, true)
    }

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
                "25",
                "--showTime",
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
    fun verifySpoiledBallotTallies() {
        verifySpoiledBallotTallies(productionGroup(), "src/commonTest/data/runWorkflowSomeAvailable")
    }
}