package electionguard.verifier

import electionguard.core.productionGroup
import kotlin.test.Test

class VerifierTest {
    @Test
    fun verificationAll() {
        runVerifier(productionGroup(), "src/commonTest/data/allAvailable", 11, true)
    }

    @Test
    fun verificationAllJson() {
        runVerifier(productionGroup(), "src/commonTest/data/allAvailableJson", 11, true)
    }

    @Test
    fun verificationSome() {
        runVerifier(productionGroup(), "src/commonTest/data/someAvailable", 11, true)
    }

    @Test
    fun verificationSomeJson() {
        runVerifier(productionGroup(), "src/commonTest/data/someAvailableJson", 11, true)
    }

    @Test
    fun readRecoveredElectionRecordAndValidate() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/someAvailable",
                "-nthreads",
                "25",
                "--showTime",
            )
        )
    }

    @Test
    fun testVerifyEncryptedBallots() {
        verifyEncryptedBallots(productionGroup(), "src/commonTest/data/someAvailable", 11)
    }

    @Test
    fun verifyDecryptedTallyWithRecoveredShares() {
        verifyDecryptedTally(productionGroup(), "src/commonTest/data/someAvailable")
    }

    @Test
    fun verifySpoiledBallotTallies() {
        verifyChallengedBallots(productionGroup(), "src/commonTest/data/someAvailable")
    }
}