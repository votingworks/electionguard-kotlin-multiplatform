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

class AddEncryptedBallotJsonTest {
    val input = "src/commonTest/data/workflow/allAvailableJson"
    val outputDirJson = "testOut/encrypt/addEncryptedBallotJson"

    val nballots = 4

    @Test
    fun testJustOne() {
        val outputDir = "$outputDirJson/testJustOne"
        val device = "device0"

        val group = productionGroup()
        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!
        val encryptor = AddEncryptedBallot(
            group,
            electionRecord.manifest(),
            electionInit,
            device,
            electionRecord.config().configBaux0,
            false,
            outputDir,
            "${outputDir}/invalidDir",
            true,
            true,
        )
        val ballotProvider = RandomBallotProvider(electionRecord.manifest())

        repeat (nballots) {
            val ballot = ballotProvider.makeBallot()
            val isOk = encryptor.encryptAndAdd(ballot, EncryptedBallot.BallotState.CAST)
            assertTrue(isOk)
        }
        encryptor.close()

        val result = makeConsumer(outputDir, group, true)
        var count = 0
        result.iterateEncryptedBallots(device) { it.state == EncryptedBallot.BallotState.CAST }.forEach {
            count++
        }
        assertEquals(nballots, count)
    }

    @Test
    fun testCallMultipleTimes() {
        val outputDir = "$outputDirJson/testCallMultipleTimes"
        val device = "device1"

        // clear output directory
        makePublisher(outputDir, true, true)

        val group = productionGroup()
        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!

        repeat(3) {
            val encryptor = AddEncryptedBallot(
                group,
                electionRecord.manifest(),
                electionInit,
                device,
                electionRecord.config().configBaux0,
                false,
                outputDir,
                "outputDir/invalidDir",
                true,
                false,
            )
            val ballotProvider = RandomBallotProvider(electionRecord.manifest())

            repeat(nballots) {
                val ballot = ballotProvider.makeBallot()
                val isOk = encryptor.encryptAndAdd(ballot, EncryptedBallot.BallotState.CAST)
                assertTrue(isOk)
            }
            encryptor.close()
        }

        val result = makeConsumer(outputDir, group, true)
        var count = 0
        result.iterateEncryptedBallots(device) { it.state == EncryptedBallot.BallotState.CAST }.forEach {
            count++
        }
        assertEquals(3 * nballots, count)
    }

    @Test
    fun testMultipleDevices() {
        val outputDir = "$outputDirJson/testMultipleDevices"

        // clear output directory
        makePublisher(outputDir, true, true)

        val group = productionGroup()
        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!

        repeat(3) { it ->
            val encryptor = AddEncryptedBallot(
                group,
                electionRecord.manifest(),
                electionInit,
                "device$it",
                electionRecord.config().configBaux0,
                false,
                outputDir,
                "$outputDir/invalidDir",
                true,
                false,
            )
            val ballotProvider = RandomBallotProvider(electionRecord.manifest())

            repeat(nballots) {
                val ballot = ballotProvider.makeBallot()
                val isOk = encryptor.encryptAndAdd(ballot, EncryptedBallot.BallotState.CAST)
                assertTrue(isOk)
            }
            encryptor.close()
        }

        val result = makeConsumer(outputDir, group, true)
        var count = 0
        result.iterateAllEncryptedBallots { it.state == EncryptedBallot.BallotState.CAST }.forEach {
            count++
        }
        assertEquals(3 * nballots, count)
    }

    @Test
    fun testOneWithChain() {
        val outputDir = "$outputDirJson/testOneWithChain"
        val device = "device0"

        val group = productionGroup()
        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!
        val publisher = makePublisher(outputDir, true, true)
        publisher.writeElectionInitialized(electionInit)

        val encryptor = AddEncryptedBallot(
            group,
            electionRecord.manifest(),
            electionInit,
            device,
            electionRecord.config().configBaux0,
            true,
            outputDir,
            "${outputDir}/invalidDir",
            true,
            false,
        )
        val ballotProvider = RandomBallotProvider(electionRecord.manifest())

        repeat(nballots) {
            val ballot = ballotProvider.makeBallot()
            val isOk = encryptor.encryptAndAdd(ballot, EncryptedBallot.BallotState.CAST)
            assertTrue(isOk)
        }
        encryptor.close()

        checkOutput(group, outputDir, nballots)
    }

    @Test
    fun testCallMultipleTimesChaining() {
        val outputDir = "$outputDirJson/testCallMultipleTimesChaining"
        val device = "device1"

        val group = productionGroup()
        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!
        val publisher = makePublisher(outputDir, true, true)
        publisher.writeElectionInitialized(electionInit)

        repeat(3) {
            val encryptor = AddEncryptedBallot(
                group,
                electionRecord.manifest(),
                electionInit,
                device,
                electionRecord.config().configBaux0,
                true,
                outputDir,
                "outputDir/invalidDir",
                true,
                false,
            )
            val ballotProvider = RandomBallotProvider(electionRecord.manifest())

            repeat(nballots) {
                val ballot = ballotProvider.makeBallot()
                val isOk = encryptor.encryptAndAdd(ballot, EncryptedBallot.BallotState.CAST)
                assertTrue(isOk)
            }
            encryptor.close()
        }

        checkOutput(group, outputDir, 3 * nballots)

    }

    @Test
    fun testMultipleDevicesChaining() {
        val outputDir = "$outputDirJson/testMultipleDevicesChaining"

        val group = productionGroup()
        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!
        val publisher = makePublisher(outputDir, true, true)
        publisher.writeElectionInitialized(electionInit)

        repeat(3) { it ->
            val encryptor = AddEncryptedBallot(
                group,
                electionRecord.manifest(),
                electionInit,
                "device$it",
                electionRecord.config().configBaux0,
                true,
                outputDir,
                "$outputDir/invalidDir",
                true,
                false,
            )
            val ballotProvider = RandomBallotProvider(electionRecord.manifest())

            repeat(nballots) {
                val ballot = ballotProvider.makeBallot()
                val isOk = encryptor.encryptAndAdd(ballot, EncryptedBallot.BallotState.CAST)
                assertTrue(isOk)
            }
            encryptor.close()
        }

        checkOutput(group, outputDir, 3 * nballots)

    }
}