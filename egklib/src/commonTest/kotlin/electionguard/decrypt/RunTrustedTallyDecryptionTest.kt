package electionguard.decrypt

import electionguard.core.productionGroup

import kotlin.test.Test

/** Test Decryption with in-process DecryptingTrustee's. */
class RunTrustedTallyDecryptionTest {
    @Test
    fun testDecryptionAll() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/workflow/allAvailableProto"
        val trusteeDir = "$inputDir/private_data/trustees"
        val outputDir = "testOut/decrypt/testDecryptionAll"
        println("testDecryptionAll input= $inputDir\n   trustees= $trusteeDir\n   output = $outputDir")
        runDecryptTally(group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir), "createdBy")
    }

    @Test
    fun testDecryptionAllJson() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/workflow/allAvailableJson",
                "-trustees",
                "src/commonTest/data/workflow/allAvailableJson/private_data/trustees",
                "-out",
                "testOut/decrypt/testDecryptionJson",
                "-createdBy",
                "RunTrustedTallyDecryptionTest",
            )
        )
    }

    @Test
    fun testDecryptionSome() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/workflow/someAvailableProto",
                "-trustees",
                "src/commonTest/data/workflow/someAvailableProto/private_data/trustees",
                "-out",
                "testOut/decrypt/testDecryptionSome",
                "-createdBy",
                "RunTrustedTallyDecryptionTest",
                "-missing",
                "1,4"
            )
        )
    }


    @Test
    fun testDecryptionSomeJson() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/workflow/someAvailableJson",
                "-trustees",
                "src/commonTest/data/workflow/someAvailableJson/private_data/trustees",
                "-out",
                "testOut/decrypt/testDecryptionSome",
                "-createdBy",
                "RunTrustedTallyDecryptionTest",
                "-missing",
                "1,4"
            )
        )
    }
}
