package electionguard.encrypt

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
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
        val publisher = makePublisher(outputDir, true, false)
        publisher.writeElectionInitialized(electionInit)

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
            false,
        )
        val ballotProvider = RandomBallotProvider(electionRecord.manifest())

        repeat(nballots) {
            val ballot = ballotProvider.makeBallot()
            val result = encryptor.encrypt(ballot)
            assertTrue(result is Ok)
            encryptor.submit(result.unwrap().confirmationCode, EncryptedBallot.BallotState.CAST)
        }
        encryptor.close()

        checkOutput(group, outputDir, nballots, false)
    }

    @Test
    fun testCallMultipleTimes() {
        val outputDir = "$outputDirProto/testCallMultipleTimes"
        val device = "device1"

        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!
        val publisher = makePublisher(outputDir, true, false)
        publisher.writeElectionInitialized(electionInit)

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
                val result = encryptor.encrypt(ballot)
                assertTrue(result is Ok)
                encryptor.submit(result.unwrap().confirmationCode, EncryptedBallot.BallotState.CAST)
            }
            encryptor.close()
        }

        checkOutput(group, outputDir, 3 * nballots, false)
    }

    @Test
    fun testMultipleDevices() {
        val outputDir = "$outputDirProto/testMultipleDevices"

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
                false,
                outputDir,
                "$outputDir/invalidDir",
                false,
                false,
            )
            val ballotProvider = RandomBallotProvider(electionRecord.manifest())

            repeat(nballots) {
                val ballot = ballotProvider.makeBallot()
                val result = encryptor.encrypt(ballot)
                assertTrue(result is Ok)
                encryptor.submit(result.unwrap().confirmationCode, EncryptedBallot.BallotState.CAST)
            }
            encryptor.close()
        }

        checkOutput(group, outputDir, 3 * nballots, false)
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
            val result = encryptor.encrypt(ballot)
            assertTrue(result is Ok)
            encryptor.submit(result.unwrap().confirmationCode, EncryptedBallot.BallotState.CAST)
        }
        encryptor.close()

        checkOutput(group, outputDir, nballots, true)
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
                val result = encryptor.encrypt(ballot)
                assertTrue(result is Ok)
                encryptor.submit(result.unwrap().confirmationCode, EncryptedBallot.BallotState.CAST)
            }
            encryptor.close()
        }

        checkOutput(group, outputDir, 4 * nballots, true)
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
                val result = encryptor.encrypt(ballot)
                assertTrue(result is Ok)
                encryptor.submit(result.unwrap().confirmationCode, EncryptedBallot.BallotState.CAST)
            }
            encryptor.close()
        }

        checkOutput(group, outputDir, 3 * nballots, true)
    }
}

fun checkOutput(group : GroupContext, outputDir: String, expectedCount: Int, chained : Boolean = false) {
    val consumer = makeConsumer(outputDir, group, false)
    var count = 0
    consumer.iterateAllEncryptedBallots { true }.forEach {
        count++
    }
    assertEquals(expectedCount, count)

    consumer.encryptingDevices().forEach { device ->
        val chain = consumer.readEncryptedBallotChain(device).unwrap()
        var lastConfirmationCode: UInt256? = null
        consumer.iterateEncryptedBallots(device, null).forEach { eballot ->
            assertTrue(chain.ballotIds.contains(eballot.ballotId))
            lastConfirmationCode = eballot.confirmationCode
        }
        assertEquals(lastConfirmationCode, chain.lastConfirmationCode)
    }

    val record = readElectionRecord(consumer)
    val verifier = VerifyEncryptedBallots(group, record.manifest(),
        ElGamalPublicKey(record.jointPublicKey()!!),
        record.extendedBaseHash()!!,
        record.config(), 1)
    if (chained) {
        val result = verifier.verifyConfirmationChain(record)
        if (result is Err) {
            println("FAIL $result")
        }
        assertTrue(result is Ok)
    }
}