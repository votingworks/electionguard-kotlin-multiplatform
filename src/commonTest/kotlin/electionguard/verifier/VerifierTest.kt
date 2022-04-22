package electionguard.verifier

import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.publish.ElectionRecord
import kotlin.test.Test
import kotlin.test.assertTrue

class VerifierTest {
    //val topdir = "/home/snake/tmp/electionguard/kotlin/runDecryptingMediator"
    val topdir = "src/commonTest/data/workflow/runDecryptingMediator"
    @Test
    fun readElectionRecordAndValidate() {
        runTest {
            val group = productionGroup()
            val electionRecordIn = ElectionRecord(topdir, group)
            val verifier = Verifier(group, electionRecordIn)

            val guardiansOk = verifier.verifyGuardianPublicKey()
            println("verifyGuardianPublicKey $guardiansOk")

            val ballotsOk = verifier.verifySubmittedBallots(electionRecordIn.iterateSubmittedBallots())
            println("verifySubmittedBallots $ballotsOk")

            val tallyOk = verifier.verifyDecryptedTally()
            println("verifyDecryptedTally $tallyOk")

            val allOk = guardiansOk && ballotsOk && tallyOk
            assertTrue(allOk)
        }
    }

}