package electionguard.verifier

import electionguard.core.productionGroup
import electionguard.core.runTest
import kotlin.test.Test

class VerifierTest {
    @Test
    fun readElectionRecordAndValidate() {
        runTest {
            runVerifier(productionGroup(), "src/commonTest/data/runWorkflowAllAvailable")
        }
    }

    @Test
    fun readRecoveredElectionRecordAndValidate() {
        runTest {
            runVerifier(productionGroup(), "src/commonTest/data/runWorkflowSomeAvailable")
        }
    }

}