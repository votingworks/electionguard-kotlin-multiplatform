package electionguard.decryptBallot

import electionguard.core.productionGroup
import electionguard.decrypt.readDecryptingTrustees
import electionguard.publish.makeConsumer

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test runDecryptBallots with in-process DecryptingTrustee's. Do not use this in production.
 * Note that when the election record changes, the test dataset must be regenerated. For this test, we need to:
 *   1. run showBallotIds() test to see what the possible ballot ids are
 *   2. modify inputDir/private_data/wantedBallots.txt and add 2 valid ballot ids
 *   3. modify testDecryptBallotsSome() and add 3 valid ballot ids to the command line argument
 */
class RunDecryptBallotsTest {
    val nthreads = 25

    @Test
    fun testDecryptBallotsAll() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/allAvailable"
        val trusteeDir = "src/commonTest/data/allAvailable/private_data/trustees"
        val outputDir = "testOut/testDecryptingBallotsAll"
        println("\ntestDecryptBallotsAll")
        val n = runDecryptBallots(
            group,
            inputDir,
            outputDir,
            readDecryptingTrustees(group, inputDir, trusteeDir),
            "ALL",
            nthreads,
        )
        assertEquals(25, n)
    }

    @Test
    fun testDecryptBallotsSomeFromList() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/someAvailable"
        val trusteeDir = "src/commonTest/data/someAvailable/private_data/trustees"
        val outputDir = "testOut/testDecryptBallotsSomeFromList"
        println("\ntestDecryptBallotsSomeFromList")
        val n = runDecryptBallots(
            group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir, "5"),
            "ballot-id--1232080716," +
                    "ballot-id-1773225402," +
                    "ballot-id-869783002",
            3,
        )
        assertEquals(3, n)
    }

    @Test
    fun testDecryptBallotsSomeFromFile() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/someAvailable"
        val trusteeDir = "src/commonTest/data/someAvailable/private_data/trustees"
        val wantBallots = "src/commonTest/data/someAvailable/private_data/wantedBallots.txt"
        val outputDir = "testOut/testDecryptingBallotsSomeFromFile"
        println("\ntestDecryptBallotsSomeFromFile")
        val n = runDecryptBallots(
            group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir, "4,5"),
            wantBallots,
            2,
        )
        assertEquals(2, n)
    }

    // decrypt all the ballots
    @Test
    fun testDecryptBallotsMainMultiThreaded() {
        println("\ntestDecryptBallotsMainMultiThreaded")
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/someAvailable",
                "-trustees",
                "src/commonTest/data/someAvailable/private_data/trustees",
                "-out",
                "testOut/testDecryptingBallotsSome",
                "-spoiled",
                "all",
                "-nthreads",
                "$nthreads"
            )
        )
    }

    // decrypt the ballots marked spoiled
    @Test
    fun testDecryptBallotsMarkedSpoiled() {
        println("\ntestDecryptBallotsMainDefault")
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/someAvailable",
                "-trustees",
                "src/commonTest/data/someAvailable/private_data/trustees",
                "-out",
                "testOut/testDecryptingBallotsSome",
                "-nthreads",
                "1"
            )
        )
    }

    @Test
    fun showBallotIds() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/someAvailable"
        val ballotDir = "src/commonTest/data/someAvailable/private_data/input/"
        val consumerIn = makeConsumer(inputDir, group)

        consumerIn.iteratePlaintextBallots(ballotDir, null).forEach {
            println(it.ballotId)
        }
    }
}
