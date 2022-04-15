package electionguard.verifier

import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.publish.Consumer
import kotlin.test.Test
import kotlin.test.assertTrue

class VerifierTest {
    @Test
    fun readElectionRecordAndValidate() {
        runTest {
            val group = productionGroup()
            val consumer = Consumer("/home/snake/tmp/electionguard/kotlin/runDecryptingMediator", group)
            val electionRecord = consumer.readElectionRecordAllData()
            val verifier = Verifier(group, electionRecord)

            val guardiansOk = verifier.verifyGuardianPublicKey()
            println("verifyGuardianPublicKey $guardiansOk")

            val ballotsOk = verifier.verifySubmittedBallots()
            println("verifySubmittedBallots $ballotsOk")

            val tallyOk = verifier.verifyDecryptedTally()
            println("verifyDecryptedTally $tallyOk")

            val allOk = guardiansOk && ballotsOk && tallyOk
            assertTrue(allOk)
        }
    }

}