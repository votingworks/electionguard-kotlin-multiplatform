package electionguard.encrypt

import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedBallot
import electionguard.core.*
import electionguard.input.RandomBallotProvider
import electionguard.publish.ElectionRecordProtoPaths
import electionguard.publish.makeConsumer
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AddEncryptedBallotTest {
    val input = "src/commonTest/data/workflow/allAvailableProto"
    val outputDirProto = "testOut/encrypt/addEncryptedBallot"

    val nballots = 4

    @Test
    fun testJustOne() {
        val outputDir = "$outputDirProto/testJustOne"
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
                false,
                true,
            )
            val ballotProvider = RandomBallotProvider(electionRecord.manifest())

            repeat(nballots) {
                val ballot = ballotProvider.makeBallot()
                val isOk = encryptor.encryptAndAdd(ballot, EncryptedBallot.BallotState.CAST)
                println(" write ${ballot.ballotId}")
                assertTrue(isOk)
            }
            encryptor.close()

        val result = makeConsumer(outputDir, group, false)
        var count = 0
        result.iterateEncryptedBallots(device) { it.state == EncryptedBallot.BallotState.CAST }.forEach {
            println(" read ${it.ballotId}")
            count++
        }
        assertEquals(nballots, count)
    }

    @Test
    fun testCallMultipleTimes() {
        val outputDir = "$outputDirProto/testCallMultipleTimes"
        val device = "device1"

        // clear output directory
        makePublisher(outputDir, true, false)

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

        val result = makeConsumer(outputDir, group, false)
        var count = 0
        result.iterateEncryptedBallots(device) { it.state == EncryptedBallot.BallotState.CAST }.forEach {
            println(" read ${it.ballotId}")
            count++
        }
        assertEquals(3 * nballots, count)
    }

    @Test
    fun testMultipleDevices() {
        val outputDir = "$outputDirProto/testMultipleDevices"

        // clear output directory
        makePublisher(outputDir, true, false)

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

        val result = makeConsumer(outputDir, group, false)
        var count = 0
        result.iterateAllEncryptedBallots { it.state == EncryptedBallot.BallotState.CAST }.forEach {
            println(" read ${it.ballotId}")
            count++
        }
        assertEquals(3 * nballots, count)
    }

    @Test
    fun testOneWithChain() {
        val outputDir = "$outputDirProto/testOneWithChain"
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
            true,
            outputDir,
            "${outputDir}/invalidDir",
            false,
            true,
        )
        val ballotProvider = RandomBallotProvider(electionRecord.manifest())

        repeat(nballots) {
            val ballot = ballotProvider.makeBallot()
            val isOk = encryptor.encryptAndAdd(ballot, EncryptedBallot.BallotState.CAST)
            println(" write ${ballot.ballotId}")
            assertTrue(isOk)
        }
        encryptor.close()

        val consumer = makeConsumer(outputDir, group, false)
        var count = 0
        consumer.iterateEncryptedBallots(device) { it.state == EncryptedBallot.BallotState.CAST }.forEach {
            println(" read ${it.ballotId}")
            count++
        }
        assertEquals(nballots, count)

        // test the chain
        val chain = consumer.readEncryptedBallotChain(device).unwrap()
        consumer.iterateEncryptedBallots(device, null).forEach { eballot ->
            assertTrue(chain.ballotIds.contains(eballot.ballotId))
            assertTrue(chain.confirmationCodes.contains(eballot.confirmationCode))
            println(" ${eballot.ballotId} has code ${eballot.confirmationCode}")
        }
    }

    @Test
    fun testCallMultipleTimesChaining() {
        val outputDir = "$outputDirProto/testCallMultipleTimesChaining"
        val device = "device1"

        // clear output directory
        makePublisher(outputDir, true, false)

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
                true,
                outputDir,
                "outputDir/invalidDir",
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

        val consumer = makeConsumer(outputDir, group, false)
        var count = 0
        consumer.iterateEncryptedBallots(device) { it.state == EncryptedBallot.BallotState.CAST }.forEach {
            println(" read ${it.ballotId}")
            count++
        }
        assertEquals(3 * nballots, count)

        // test the chain
        val chain = consumer.readEncryptedBallotChain(device).unwrap()
        consumer.iterateEncryptedBallots(device, null).forEach { eballot ->
            assertTrue(chain.ballotIds.contains(eballot.ballotId))
            assertTrue(chain.confirmationCodes.contains(eballot.confirmationCode))
            println(" ${eballot.ballotId} has code ${eballot.confirmationCode}")
        }
    }

    @Test
    fun testMultipleDevicesChaining() {
        val outputDir = "$outputDirProto/testMultipleDevicesChaining"

        // clear output directory
        makePublisher(outputDir, true, false)

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
                true,
                outputDir,
                "$outputDir/invalidDir",
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

        val consumer = makeConsumer(outputDir, group, false)
        var count = 0
        consumer.iterateAllEncryptedBallots { it.state == EncryptedBallot.BallotState.CAST }.forEach {
            println(" read ${it.ballotId}")
            count++
        }
        assertEquals(3 * nballots, count)

        consumer.encryptingDevices().forEach { device ->
            val chain = consumer.readEncryptedBallotChain(device).unwrap()
            consumer.iterateEncryptedBallots(device, null).forEach { eballot ->
                assertTrue(chain.ballotIds.contains(eballot.ballotId))
                assertTrue(chain.confirmationCodes.contains(eballot.confirmationCode))
                println(" ${eballot.ballotId} has code ${eballot.confirmationCode}")
            }
        }
    }

}