package electionguard.workflow

import electionguard.core.productionGroup
import electionguard.decrypt.readDecryptingTrustees
import electionguard.decrypt.runDecryptingMediator

import kotlin.test.Test

/** Test DecryptingMediator with in-process DecryptingTrustee's. Cannot use this in production */
class RunDecryptingMediatorTest {
    @Test
    fun testDecryptingMediatorAll() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/runWorkflowAllAvailable"
        val trusteeDir = "src/commonTest/data/runWorkflowAllAvailable/private_data/trustees"
        val outputDir = "testOut/testDecryptingMediator"
        runDecryptingMediator(group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir))
    }

    @Test
    fun testDecryptingMediatorSome() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/runWorkflowSomeAvailable"
        val trusteeDir = "src/commonTest/data/runWorkflowSomeAvailable/private_data/trustees"
        val outputDir = "testOut/testDecryptingMediator"
        runDecryptingMediator(group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir))
    }
}
