package electionguard.verifier

import electionguard.cli.RunVerifier
import electionguard.core.productionGroup
import kotlin.test.Test

class VerifierTest {
    @Test
    fun verificationAll() {
        RunVerifier.runVerifier(productionGroup(), "src/commonTest/data/workflow/allAvailableProto", 11, true)
    }

    @Test
    fun verificationAllJson() {
        RunVerifier.runVerifier(productionGroup(), "src/commonTest/data/workflow/allAvailableJson", 11, true)
    }

    @Test
    fun verificationSome() {
        RunVerifier.runVerifier(productionGroup(), "src/commonTest/data/workflow/someAvailableProto", 11, true)
    }

    @Test
    fun verificationSomeJson() {
        RunVerifier.runVerifier(productionGroup(), "src/commonTest/data/workflow/someAvailableJson", 11, true)
    }

    @Test
    fun readRecoveredElectionRecordAndValidate() {
        RunVerifier.main(
            arrayOf(
                "-in",
                "src/commonTest/data/workflow/someAvailableProto",
                "-nthreads",
                "11",
                "--showTime",
            )
        )
    }

    @Test
    fun testVerifyEncryptedBallots() {
        RunVerifier.verifyEncryptedBallots(productionGroup(), "src/commonTest/data/workflow/someAvailableProto", 11)
    }

    @Test
    fun verifyDecryptedTallyWithRecoveredShares() {
        RunVerifier.verifyDecryptedTally(productionGroup(), "src/commonTest/data/workflow/someAvailableProto")
    }

    @Test
    fun verifySpoiledBallotTallies() {
        RunVerifier.verifyChallengedBallots(productionGroup(), "src/commonTest/data/workflow/chainedProto")
        RunVerifier.verifyChallengedBallots(productionGroup(), "src/commonTest/data/workflow/chainedJson")
    }

    // • Ordered lists of the ballots encrypted by each device. spec 1.9, p.42
    @Test
    fun testVerifyTallyBallotIds() {
        RunVerifier.verifyTallyBallotIds(productionGroup(), "testOut/workflow/allAvailableProto")
        RunVerifier.verifyTallyBallotIds(productionGroup(), "testOut/workflow/someAvailableProto")
        RunVerifier.verifyTallyBallotIds(productionGroup(), "testOut/workflow/allAvailableJson")
        RunVerifier.verifyTallyBallotIds(productionGroup(), "testOut/workflow/someAvailableJson")
    }
}