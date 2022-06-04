package electionguard.decryptBallot

import electionguard.core.productionGroup
import electionguard.decrypt.readDecryptingTrustees

import kotlin.test.Test
import kotlin.test.assertEquals

/** Test DecryptingMediator with in-process DecryptingTrustee's. Cannot use this in production */
class RunDecryptBallotsTest {
    @Test
    fun testDecryptingBallotsAll() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/runWorkflowAllAvailable"
        val trusteeDir = "src/commonTest/data/runWorkflowAllAvailable/private_data/trustees"
        val outputDir = "testOut/testDecryptingBallotsAll"
        val n = runDecryptBallots(
            group,
            inputDir,
            outputDir,
            readDecryptingTrustees(group, inputDir, trusteeDir),
            "ALL",
            11
        )
        assertEquals(100, n)
    }

    @Test
    fun testDecryptingBallotsSome() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/runWorkflowSomeAvailable"
        val trusteeDir = "src/commonTest/data/runWorkflowSomeAvailable/private_data/trustees"
        val outputDir = "testOut/testDecryptingBallotsSome"
        val n = runDecryptBallots(
            group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir),
            "ballot-id--171736245,ballot-id--1373972434,ballot-id-569389004",
            11
        )
        assertEquals(3, n)
    }

    @Test
    fun testDecryptingBallotsSomeList() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/runWorkflowSomeAvailable"
        val trusteeDir = "src/commonTest/data/runWorkflowSomeAvailable/private_data/trustees"
        val wantBallots = "src/commonTest/data/runWorkflowSomeAvailable/private_data/wantedBallots.txt"
        val outputDir = "testOut/testDecryptingBallotsSome"
        val n = runDecryptBallots(
            group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir),
            wantBallots,
            11
        )
        assertEquals(2, n)
    }

    @Test
    fun testDecryptingBallotsMain() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/runWorkflowSomeAvailable",
                "-trustees",
                "src/commonTest/data/runWorkflowSomeAvailable/private_data/trustees",
                "-out",
                "testOut/testDecryptingBallotsSome",
            )
        )
    }
}
