package electionguard.decrypt

import electionguard.core.productionGroup

import kotlin.test.Test

/** Test Decryption with in-process DecryptingTrustee's. */
class RunDecryptionTest {
    @Test
    fun testDecryptionAll() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/runWorkflowAllAvailable"
        val trusteeDir = "src/commonTest/data/runWorkflowAllAvailable/private_data/trustees"
        val outputDir = "testOut/testDecryptingMediatorAll"
        runDecryptTally(group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir), "createdBy")
    }

    @Test
    fun testDecryptionSome() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/runWorkflowSomeAvailable",
                "-trustees",
                "src/commonTest/data/runWorkflowSomeAvailable/private_data/trustees",
                "-out",
                "testOut/testDecryptingMediatorSome",
                "-createdBy",
                "testDecryptingMediatorSome"
            )
        )
    }
}
