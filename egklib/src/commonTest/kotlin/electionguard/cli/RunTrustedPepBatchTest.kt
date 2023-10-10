package electionguard.cli

import kotlin.test.Test

/** Test Decryption with in-process DecryptingTrustee's. */
class RunTrustedPepBatchTest {

    @Test
    fun testPepAllJson() {
        val inputDir = "src/commonTest/data/workflow/allAvailableJson"
        val ballotDir = "$inputDir/private_data/input/"
        val outputDir = "testOut/pep/testPepAllJson"
        val scannedDir = "$outputDir/scanned"
        val invalidDir = "$outputDir/invalid"

        // run the extra encryption
        RunBatchEncryption.main(
            arrayOf(
                "-in", inputDir,
                "-ballots", ballotDir,
                "-out", scannedDir,
                "-invalid", invalidDir,
                "-device", "scanned",
                "--cleanOutput",
            )
        )

        RunTrustedPep.main(
            arrayOf(
                "-in", inputDir,
                "-trustees", "src/commonTest/data/workflow/allAvailableJson/private_data/trustees",
                "-scanned", "$scannedDir/encrypted_ballots/scanned/",
                "-out", outputDir,
            )
        )

        RunVerifyPep.main(
            arrayOf(
                "-in", inputDir,
                "-pep", outputDir,
            )
        )
    }

    @Test
    fun testPepSomeJson() {
        val inputDir = "src/commonTest/data/workflow/someAvailableJson"
        val ballotDir = "$inputDir/private_data/input/"
        val outputDir = "testOut/pep/testPepSomeJson"
        val scannedDir = "$outputDir/scanned"
        val invalidDir = "$outputDir/invalid"

        // run the extra encryption
        RunBatchEncryption.main(
            arrayOf(
                "-in", inputDir,
                "-ballots", ballotDir,
                "-out", scannedDir,
                "-invalid", invalidDir,
                "-device", "scanned",
                "--cleanOutput",
            )
        )

        RunTrustedPep.main(
            arrayOf(
                "-in", inputDir,
                "-trustees", "src/commonTest/data/workflow/someAvailableJson/private_data/trustees",
                "-scanned", "$scannedDir/encrypted_ballots/scanned/",
                "-out", outputDir,
                "-missing", "1,4"
            )
        )

        RunVerifyPep.main(
            arrayOf(
                "-in", inputDir,
                "-pep", outputDir,
            )
        )
    }

    // @Test
    fun testVerifyPep() {
        val inputDir = "src/commonTest/data/workflow/allAvailableJson"
        val outputDir = "../testOut/pep/testPepAllJson"

        RunVerifyPep.main(
            arrayOf(
                "-in", inputDir,
                "-pep", "$outputDir",
            )
        )
    }


}
