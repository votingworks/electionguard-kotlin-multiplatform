package electionguard.verifier

import electionguard.cli.RunVerifier
import electionguard.core.productionGroup
import kotlin.test.Test

class VerifierTest {

    @Test
    fun verifyRemoteWorkflow() {
        try {
            RunVerifier.runVerifier(
                productionGroup(),
                "src/commonTest/data/testElectionRecord/remoteWorkflow/keyceremony",
                11
            )
            RunVerifier.runVerifier(
                productionGroup(),
                "src/commonTest/data/testElectionRecord/remoteWorkflow/electionRecord",
                11
            )
        } catch (t :Throwable) {
            t.printStackTrace(System.out)
        }
        // RunVerifier.runVerifier(productionGroup(), "/home/stormy/dev/github/egk-webapps/testOut/remoteWorkflow/keyceremony/", 11)
        // RunVerifier.runVerifier(productionGroup(), "/home/stormy/dev/github/egk-webapps/testOut/remoteWorkflow/electionRecord/", 11)
    }

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

    // @Test
    fun testProblem() {
        RunVerifier.runVerifier(productionGroup(), "../testOut/cliWorkflow/electionRecord", 11, true)
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

    // Ordered lists of the ballots encrypted by each device. spec 2.0, section 3.7, p.46
    @Test
    fun testVerifyTallyBallotIds() {
        RunVerifier.verifyTallyBallotIds(productionGroup(), "src/commonTest/data/workflow/allAvailableProto")
        RunVerifier.verifyTallyBallotIds(productionGroup(), "src/commonTest/data/workflow/someAvailableProto")
        RunVerifier.verifyTallyBallotIds(productionGroup(), "src/commonTest/data/workflow/allAvailableJson")
        RunVerifier.verifyTallyBallotIds(productionGroup(), "src/commonTest/data/workflow/someAvailableJson")
    }
}