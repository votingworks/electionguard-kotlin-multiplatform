package electionguard.verifier

import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.publish.ElectionRecord
import kotlin.test.Test
import kotlin.test.assertTrue

class VerifierTest {
    @Test
    fun readElectionRecordAndValidate() {
        runTest {
            val group = productionGroup()
            val electionRecordIn = ElectionRecord("src/commonTest/data/runWorkflowAllAvailable", group)
            val verifier = Verifier(group, electionRecordIn)

            val guardiansOk = verifier.verifyGuardianPublicKey()
            println("verifyGuardianPublicKey $guardiansOk\n")

            val ballotsOk = verifier.verifySubmittedBallots(electionRecordIn.iterateSubmittedBallots())
            println("verifySubmittedBallots $ballotsOk\n")

            val tallyOk = verifier.verifyDecryptedTally()
            println("verifyDecryptedTally $tallyOk\n")

            val allOk = guardiansOk && ballotsOk && tallyOk
            assertTrue(allOk)
        }
    }

    @Test
    fun readRecoveredElectionRecordAndValidate() {
        runTest {
            val group = productionGroup()
            val electionRecordIn = ElectionRecord("src/commonTest/data/runWorkflowSomeAvailable", group)
            val verifier = Verifier(group, electionRecordIn)

            val guardiansOk = verifier.verifyGuardianPublicKey()
            println("verifyGuardianPublicKey $guardiansOk\n")

            val ballotsOk = verifier.verifySubmittedBallots(electionRecordIn.iterateSubmittedBallots())
            println("verifySubmittedBallots $ballotsOk\n")

            val tallyOk = verifier.verifyDecryptedTally()
            println("verifyDecryptedTally $tallyOk\n")

            val allOk = guardiansOk && ballotsOk && tallyOk
            assertTrue(allOk)
        }
    }

}