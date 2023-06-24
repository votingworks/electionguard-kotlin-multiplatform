package electionguard.verifier

import electionguard.core.productionGroup
import kotlin.test.Test

class VerifierTest {
    @Test
    fun verificationOrg() {
        runVerifier(productionGroup(), "src/commonTest/data/someAvailable", 11, true)
    }

    @Test
    fun verificationJson() {
        runVerifier(productionGroup(), "testOut/RunElectionRecordConvertJson", 11, true)
    }

    @Test
    fun verificationRoundtrip() {
        runVerifier(productionGroup(), "testOut/RunElectionRecordConvertProto", 11, true)
    }

    @Test
    fun verification1() {
        runVerifier(productionGroup(), "src/commonTest/data/start", 1, true)
    }

    @Test
    fun verification23() {
        runVerifier(productionGroup(), "testOut/RunKeyCeremonyTest", 1, true)
    }

    @Test
    fun verificationChained() {
        runVerifier(productionGroup(), "testOut/testRunBatchEncryptionChain", 11, true)
    }

    @Test
    fun verificationTally() {
        runVerifier(productionGroup(), "testOut/runTallyAccumulationTest", 11, true)
    }

    @Test
    fun readElectionRecordAndValidate() {
        runVerifier(productionGroup(), "src/commonTest/data/allAvailable", 11, true)
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
        verifySpoiledBallotTallies(productionGroup(), "src/commonTest/data/someAvailable")
    }
}