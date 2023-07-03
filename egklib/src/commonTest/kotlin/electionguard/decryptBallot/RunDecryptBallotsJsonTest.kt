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
class RunDecryptBallotsJsonTest {
    val nthreads = 25

    @Test
    fun testDecryptBallotsAll() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/allAvailableJson"
        val trusteeDir = "src/commonTest/data/allAvailableJson/private_data/trustees"
        val outputDir = "testOut/decrypt/testDecryptBallotsAllJson"
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
    fun testDecryptBallotsSome() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/someAvailableJson"
        val trusteeDir = "src/commonTest/data/someAvailableJson/private_data/trustees"
        val outputDir = "testOut/decrypt/testDecryptBallotsSomeJson"
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
        val inputDir = "src/commonTest/data/someAvailableJson"
        val trusteeDir = "src/commonTest/data/someAvailableJson/private_data/trustees"
        val outputDir = "testOut/decrypt/testDecryptBallotsSomeFromListJson"
        println("\ntestDecryptBallotsSomeFromList")
        val n = runDecryptBallots(
            group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir, "5"),
            "id-1423442691," +
                    "id822115866," +
                    "id-1423774858," +
                    "id-1469219587,",
            3,
        )
        assertEquals(4, n)
    }

    @Test
    fun showBallotIds() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/someAvailableJson"
        val ballotDir = "src/commonTest/data/someAvailableJson/private_data/input/"
        val consumerIn = makeConsumer(inputDir, group)

        consumerIn.iteratePlaintextBallots(ballotDir, null).forEach {
            println(it.ballotId)
        }
    }
}
