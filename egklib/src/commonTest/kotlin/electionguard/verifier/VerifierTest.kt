package electionguard.verifier

import electionguard.core.productionGroup
import kotlin.test.Test

class VerifierTest {
    @Test
    fun verificationAll() {
        runVerifier(productionGroup(), "src/commonTest/data/workflow/allAvailableProto", 11, true)
    }

    @Test
    fun verificationAllJson() {
        runVerifier(productionGroup(), "src/commonTest/data/workflow/allAvailableJson", 11, true)
    }

    @Test
    fun verificationSome() {
        runVerifier(productionGroup(), "src/commonTest/data/workflow/someAvailableProto", 11, true)
    }

    @Test
    fun verificationSomeJson() {
        runVerifier(productionGroup(), "src/commonTest/data/workflow/someAvailableJson", 11, true)
    }

    @Test
    fun readRecoveredElectionRecordAndValidate() {
        main(
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
        verifyEncryptedBallots(productionGroup(), "src/commonTest/data/workflow/someAvailableProto", 11)
    }

    @Test
    fun verifyDecryptedTallyWithRecoveredShares() {
        verifyDecryptedTally(productionGroup(), "src/commonTest/data/workflow/someAvailableProto")
    }

    @Test
    fun verifySpoiledBallotTallies() {
        verifyChallengedBallots(productionGroup(), "src/commonTest/data/workflow/chainedProto")
        verifyChallengedBallots(productionGroup(), "src/commonTest/data/workflow/chainedJson")
    }

    // • Ordered lists of the ballots encrypted by each device. spec 1.9, p.42
    @Test
    fun testVerifyTallyBallotIds() {
        verifyTallyBallotIds(productionGroup(), "testOut/workflow/allAvailableProto")
        verifyTallyBallotIds(productionGroup(), "testOut/workflow/someAvailableProto")
        verifyTallyBallotIds(productionGroup(), "testOut/workflow/allAvailableJson")
        verifyTallyBallotIds(productionGroup(), "testOut/workflow/someAvailableJson")
    }
}