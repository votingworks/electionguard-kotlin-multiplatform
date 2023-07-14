package electionguard.encrypt

import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedBallot
import electionguard.core.*
import electionguard.input.RandomBallotProvider
import electionguard.publish.makeConsumer
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import electionguard.verifier.VerifyEncryptedBallots
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddEncryptedBallotTest {
    val group = productionGroup()
    val input = "src/commonTest/data/workflow/allAvailableProto"
    val outputDirProto = "testOut/encrypt/addEncryptedBallot"

    val nballots = 4

    @Test
    fun testJustOne() {
        val outputDir = "$outputDirProto/testJustOne"
        val device = "device0"

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
            assertTrue(isOk)
        }
        encryptor.close()

        val result = makeConsumer(outputDir, group, false)
        var count = 0
        result.iterateEncryptedBallots(device) { it.state == EncryptedBallot.BallotState.CAST }.forEach {
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
                assertTrue(isOk)
            }
            encryptor.close()
        }

        val result = makeConsumer(outputDir, group, false)
        var count = 0
        result.iterateEncryptedBallots(device) { it.state == EncryptedBallot.BallotState.CAST }.forEach {
            count++
        }
        assertEquals(3 * nballots, count)
    }

    @Test
    fun testMultipleDevices() {
        val outputDir = "$outputDirProto/testMultipleDevices"

        // clear output directory
        makePublisher(outputDir, true, false)

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
                assertTrue(isOk)
            }
            encryptor.close()
        }

        val result = makeConsumer(outputDir, group, false)
        var count = 0
        result.iterateAllEncryptedBallots { it.state == EncryptedBallot.BallotState.CAST }.forEach {
            count++
        }
        assertEquals(3 * nballots, count)
    }

    @Test
    fun testOneWithChain() {
        val outputDir = "$outputDirProto/testOneWithChain"
        val device = "device0"

        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!
        val publisher = makePublisher(outputDir, true, false)
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
            false,
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
        val outputDir = "$outputDirProto/testCallMultipleTimesChaining"
        val device = "device1"

        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!
        val publisher = makePublisher(outputDir, true, false)
        publisher.writeElectionInitialized(electionInit)

        repeat(4) {
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
                assertTrue(isOk)
            }
            encryptor.close()
        }

        checkOutput(group, outputDir, 4 * nballots)
    }

    @Test
    fun testMultipleDevicesChaining() {
        val outputDir = "$outputDirProto/testMultipleDevicesChaining"

        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!
        val publisher = makePublisher(outputDir, true, false)
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
                false,
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

fun checkOutput(group : GroupContext, outputDir: String, expectedCount: Int) {
    val consumer = makeConsumer(outputDir, group, false)
    var count = 0
    consumer.iterateAllEncryptedBallots { it.state == EncryptedBallot.BallotState.CAST }.forEach {
        count++
    }
    assertEquals(expectedCount, count)

    consumer.encryptingDevices().forEach { device ->
        val chain = consumer.readEncryptedBallotChain(device).unwrap()
        consumer.iterateEncryptedBallots(device, null).forEach { eballot ->
            assertTrue(chain.ballotIds.contains(eballot.ballotId))
            assertTrue(chain.confirmationCodes.contains(eballot.confirmationCode))
        }
    }

    val record = readElectionRecord(consumer)
    val verifier = VerifyEncryptedBallots(group, record.manifest(),
        ElGamalPublicKey(record.jointPublicKey()!!),
        record.extendedBaseHash()!!,
        record.config(), 1)
    verifier.verifyConfirmationChain(record)
}