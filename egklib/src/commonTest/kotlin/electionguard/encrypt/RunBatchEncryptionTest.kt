package electionguard.encrypt

import electionguard.cli.RunBatchEncryption
import electionguard.cli.RunBatchEncryption.Companion.batchEncryption
import electionguard.cli.RunVerifier
import electionguard.core.productionGroup
import electionguard.input.RandomBallotProvider
import electionguard.publish.makeConsumer
import electionguard.publish.readElectionRecord
import kotlin.test.Test
import kotlin.test.assertContains

class
RunBatchEncryptionTest {
    val nthreads = 25

    @Test
    fun testRunBatchEncryptionWithJsonBallots() {
        RunBatchEncryption.main(
            arrayOf(
                "-in", "src/commonTest/data/workflow/allAvailableJson",
                "-ballots", "src/commonTest/data/fakeBallots/json",
                "-out", "testOut/encrypt/testRunBatchEncryptionWithJsonBallots",
                "-invalid", "testOut/encrypt/testRunBatchEncryptionWithJsonBallots/invalid_ballots",
                "-nthreads", "$nthreads",
                "-device", "device2",
                "--cleanOutput",
            )
        )
        RunVerifier.runVerifier(productionGroup(), "testOut/encrypt/testRunBatchEncryptionWithJsonBallots", 11)
    }

    @Test
    fun testRunBatchEncryptionJson() {
        RunBatchEncryption.main(
            arrayOf(
                "-in", "src/commonTest/data/workflow/allAvailableJson",
                "-ballots", "src/commonTest/data/fakeBallots/json",
                "-out", "testOut/encrypt/testRunBatchEncryptionJson",
                "-invalid", "testOut/encrypt/testRunBatchEncryptionJson/invalid_ballots",
                "-nthreads", "$nthreads",
                "-device", "device2",
                "--cleanOutput",
            )
        )
        RunVerifier.runVerifier(productionGroup(), "testOut/encrypt/testRunBatchEncryptionJson", 11)
    }

    @Test
    fun testRunBatchEncryptionJsonWithProtoBallots() {
        RunBatchEncryption.main(
            arrayOf(
                "-in", "src/commonTest/data/workflow/allAvailableJson",
                "-ballots", "src/commonTest/data/fakeBallots/json",
                "-out", "testOut/encrypt/testRunBatchEncryptionJsonWithProtoBallots",
                "-invalid", "testOut/encrypt/testRunBatchEncryptionJsonWithProtoBallots/invalid_ballots",
                "-nthreads", "$nthreads",
                "-device", "device3",
                "--cleanOutput",
            )
        )
        RunVerifier.runVerifier(productionGroup(), "testOut/encrypt/testRunBatchEncryptionJsonWithProtoBallots", 11)
    }

    @Test
    fun testRunBatchEncryptionEncryptTwice() {
        RunBatchEncryption.main(
            arrayOf(
                "-in", "src/commonTest/data/workflow/allAvailableJson",
                "-ballots", "src/commonTest/data/fakeBallots/json",
                "-out", "testOut/encrypt/testRunBatchEncryptionEncryptTwice",
                "-invalid", "testOut/encrypt/testRunBatchEncryptionEncryptTwice/invalid_ballots",
                "-nthreads", "$nthreads",
                "-device", "device4",
                "-check", "EncryptTwice",
                "--cleanOutput",
            )
        )
    }

    @Test
    fun testRunBatchEncryptionVerify() {
        RunBatchEncryption.main(
            arrayOf(
                "-in", "src/commonTest/data/workflow/allAvailableJson",
                "-ballots", "src/commonTest/data/fakeBallots/json",
                "-out", "testOut/encrypt/testRunBatchEncryptionVerify",
                "-invalid", "testOut/encrypt/testRunBatchEncryptionVerify/invalid_ballots",
                "-nthreads", "$nthreads",
                "-device", "device35",
                "-check", "Verify",
                "--cleanOutput",
            )
        )
    }

    @Test
    fun testRunBatchEncryptionVerifyDecrypt() {
        RunBatchEncryption.main(
            arrayOf(
                "-in", "src/commonTest/data/workflow/allAvailableJson",
                "-ballots", "src/commonTest/data/fakeBallots/json",
                "-out", "testOut/encrypt/testRunBatchEncryptionVerifyDecrypt",
                "-invalid", "testOut/encrypt/testRunBatchEncryptionVerifyDecrypt/invalid_ballots",
                "-nthreads", "$nthreads",
                "-device", "device42",
                "-check", "DecryptNonce",
                "--cleanOutput",
            )
        )
    }

    @Test
    fun testInvalidBallot() {
        val inputDir = "src/commonTest/data/workflow/allAvailableJson"
        val outputDir = "testOut/testInvalidBallot"
        val invalidDir = "testOut/testInvalidBallot/invalidDir"

        val group = productionGroup()
        val electionRecord = readElectionRecord(group, inputDir)

        val ballot = RandomBallotProvider(electionRecord.manifest(), 1).makeBallot()
        val ballots = listOf( ballot.copy(ballotStyle = "badStyleId"))

        batchEncryption(
            group,
            inputDir,
            ballots,
            device = "testDevice",
            outputDir = outputDir,
            encryptDir = null,
            invalidDir = invalidDir,
            1,
            "testInvalidBallot",
        )

        val consumerOut = makeConsumer(group, invalidDir)
        consumerOut.iteratePlaintextBallots(invalidDir, null).forEach {
            println("${it.errors}")
            assertContains(it.errors.toString(), "Ballot.A.1 Ballot Style 'badStyleId' does not exist in election")
        }

    }

}