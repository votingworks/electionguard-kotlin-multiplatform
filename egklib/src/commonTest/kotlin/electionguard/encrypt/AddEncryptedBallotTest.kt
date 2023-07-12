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
    val outputDir = "testOut/encrypt/addEncryptedBallot"

    val nballots = 4

    @Test
    fun testAddEncryptedBallot() {
        val group = productionGroup()
        val electionRecord = readElectionRecord(group, input)
        val device = "device0"
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
    fun testAddEncryptedBallotCallMultipleTimes() {
        val outputDir = "$outputDir-M"
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
                "device1",
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
        result.iterateAllEncryptedBallots { it.state == EncryptedBallot.BallotState.CAST }.forEach {
            println(" read ${it.ballotId}")
            count++
        }
        assertEquals(3 * nballots, count)
    }

    @Test
    fun testAddEncryptedBallotMultipleDevices() {
        val outputDir = "$outputDir-D"

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
                "outputDir",
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
    fun testAddEncryptedBallotWithChain() {
        val outputDir = "$outputDir-chain"
        val group = productionGroup()
        val electionRecord = readElectionRecord(group, input)
        val device = "device0"
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
    fun testAddEncryptedBallotCallMultipleTimesChained() {
        val outputDir = "$outputDir-Mchain"
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
        consumer.iterateAllEncryptedBallots { it.state == EncryptedBallot.BallotState.CAST }.forEach {
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
    fun testAddEncryptedBallotMultipleDevicesChained() {
        val outputDir = "$outputDir-Dchained"

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