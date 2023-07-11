package electionguard.encrypt

import electionguard.ballot.EncryptedBallot
import electionguard.core.*
import electionguard.input.RandomBallotProvider
import electionguard.publish.makeConsumer
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddEncryptedBallotTest {
    val input = "src/commonTest/data/workflow/allAvailableProto"
    val outputDir = "testOut/encrypt/addEncryptedBallot"
    val outputDirJson = "testOut/encrypt/addEncryptedBallotJson"

    val nballots = 4

    @Test
    fun testAddEncryptedBallot() {
        val group = productionGroup()
        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!
        val encryptor = AddEncryptedBallot(
            group,
            electionRecord.manifest(),
            electionInit,
            "device",
            electionRecord.config().baux0,
            false,
            outputDir,
            "${outputDir}/invalidDir",
            false,
            false,
        )
        val ballotProvider = RandomBallotProvider(electionRecord.manifest())

        repeat (nballots) {
            val ballot = ballotProvider.makeBallot()
            val isOk = encryptor.encryptAndAdd(ballot, EncryptedBallot.BallotState.CAST)
            println(" write ${ballot.ballotId}")
            assertTrue(isOk)
        }
        encryptor.close()

        val result = makeConsumer(outputDir, group, false)
        var count = 0
        result.iterateEncryptedBallots { it.state == EncryptedBallot.BallotState.CAST }.forEach {
            println(" read ${it.ballotId}")
            count++
        }
        assertEquals(nballots, count)
    }

    @Test
    fun testAddEncryptedBallotMultiple() {

        // clear output directory
        makePublisher("${outputDir}M", true, false)

        val group = productionGroup()
        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!

        repeat(3) {
            val encryptor = AddEncryptedBallot(
                group,
                electionRecord.manifest(),
                electionInit,
                "device",
                electionRecord.config().baux0,
                false,
                "${outputDir}M",
                "${outputDir}M/invalidDir",
                false,
                false,
            )
            val ballotProvider = RandomBallotProvider(electionRecord.manifest())

            repeat(nballots) {
                val ballot = ballotProvider.makeBallot()
                val isOk = encryptor.encryptAndAdd(ballot, EncryptedBallot.BallotState.CAST)
                println(" write ${ballot.ballotId}")
                assertTrue(isOk)
            }
            encryptor.close()
        }

        val result = makeConsumer("${outputDir}M", group, false)
        var count = 0
        result.iterateEncryptedBallots { it.state == EncryptedBallot.BallotState.CAST }.forEach {
            println(" read ${it.ballotId}")
            count++
        }
        assertEquals(3 * nballots, count)
    }

    @Test
    fun testAddEncryptedBallotToJson() {
        val group = productionGroup()
        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!
        val encryptor = AddEncryptedBallot(
            group,
            electionRecord.manifest(),
            electionInit,
            "device",
            electionRecord.config().baux0,
            false,
            outputDirJson,
            "${outputDirJson}/invalidDir",
            true,
            false,
        )
        val ballotProvider = RandomBallotProvider(electionRecord.manifest())

        repeat (nballots) {
            val ballot = ballotProvider.makeBallot()
            val isOk = encryptor.encryptAndAdd(ballot, EncryptedBallot.BallotState.CAST)
            println(" write ${ballot.ballotId}")
            assertTrue(isOk)
        }
        encryptor.close()

        val result = makeConsumer(outputDirJson, group, true)
        var count = 0
        result.iterateEncryptedBallots { it.state == EncryptedBallot.BallotState.CAST }.forEach {
            println(" read ${it.ballotId}")
            count++
        }
        assertEquals(nballots, count)
    }

    @Test
    fun testAddEncryptedBallotJsonMultiple() {

        // clear output directory
        makePublisher("${outputDirJson}M", true, true)

        val group = productionGroup()
        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!

        repeat(3) {
            val encryptor = AddEncryptedBallot(
                group,
                electionRecord.manifest(),
                electionInit,
                "device",
                electionRecord.config().baux0,
                false,
                "${outputDirJson}M",
                "${outputDirJson}M/invalidDir",
                true,
                false,
            )
            val ballotProvider = RandomBallotProvider(electionRecord.manifest())

            repeat(nballots) {
                val ballot = ballotProvider.makeBallot()
                val isOk = encryptor.encryptAndAdd(ballot, EncryptedBallot.BallotState.CAST)
                println(" write ${ballot.ballotId}")
                assertTrue(isOk)
            }
            encryptor.close()
        }

        val result = makeConsumer("${outputDirJson}M", group, true)
        var count = 0
        result.iterateEncryptedBallots { it.state == EncryptedBallot.BallotState.CAST }.forEach {
            println(" read ${it.ballotId}")
            count++
        }
        assertEquals(3 * nballots, count)
    }
}