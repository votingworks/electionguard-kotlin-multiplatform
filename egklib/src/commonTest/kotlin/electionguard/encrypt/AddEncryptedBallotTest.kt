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
import electionguard.util.Stats
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
            outputDir,
            "${outputDir}/invalidDir",
            isJson = publisher.isJson(),
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
    fun testEncryptAndCast() {
        val outputDir = "$outputDirProto/testEncryptAndCast"
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
            outputDir,
            "${outputDir}/invalidDir",
            isJson = publisher.isJson(),
        )
        val ballotProvider = RandomBallotProvider(electionRecord.manifest())

        repeat(nballots) {
            val ballot = ballotProvider.makeBallot()
            val result = encryptor.encryptAndCast(ballot)
            assertTrue(result is Ok)
            assertTrue( encryptor.submit(result.unwrap().confirmationCode, EncryptedBallot.BallotState.CAST) is Err)
        }
        encryptor.close()

        checkOutput(group, outputDir, nballots, publisher.isJson())
    }

    @Test
    fun testEncryptAndCastNoWrite() {
        val outputDir = "$outputDirProto/testEncryptAndCastNoWrite"
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
            outputDir,
            "${outputDir}/invalidDir",
            isJson = publisher.isJson(),
        )
        val ballotProvider = RandomBallotProvider(electionRecord.manifest())

        repeat(nballots) {
            val ballot = ballotProvider.makeBallot()
            val result = encryptor.encryptAndCast(ballot, false)
            assertTrue(result is Ok)
            assertTrue( encryptor.submit(result.unwrap().confirmationCode, EncryptedBallot.BallotState.CAST) is Err)
        }
        encryptor.close()

        // TODO make sure no ballots were written
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
                outputDir,
                "outputDir/invalidDir",
                isJson = publisher.isJson(),
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
                outputDir,
                "$outputDir/invalidDir",
                isJson = publisher.isJson(),
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
        val configWithChaining = electionRecord.config().copy(chainConfirmationCodes = true)
        val electionInit = electionRecord.electionInit()!!.copy(config = configWithChaining)

        val publisher = makePublisher(outputDir, true, false)
        publisher.writeElectionInitialized(electionInit)

        val encryptor = AddEncryptedBallot(
            group,
            electionRecord.manifest(),
            electionInit,
            device,
            outputDir,
            "${outputDir}/invalidDir",
            isJson = publisher.isJson(),
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
        val configWithChaining = electionRecord.config().copy(chainConfirmationCodes = true)
        val electionInit = electionRecord.electionInit()!!.copy(config = configWithChaining)

        val publisher = makePublisher(outputDir, true, false)
        publisher.writeElectionInitialized(electionInit)

        repeat(4) {
            val encryptor = AddEncryptedBallot(
                group,
                electionRecord.manifest(),
                electionInit,
                device,
                outputDir,
                "outputDir/invalidDir",
                isJson = publisher.isJson(),
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
        val configWithChaining = electionRecord.config().copy(chainConfirmationCodes = true)
        val electionInit = electionRecord.electionInit()!!.copy(config = configWithChaining)

        val publisher = makePublisher(outputDir, true, false)
        publisher.writeElectionInitialized(electionInit)

        repeat(3) { it ->
            val encryptor = AddEncryptedBallot(
                group,
                electionRecord.manifest(),
                electionInit,
                "device$it",
                outputDir,
                "$outputDir/invalidDir",
                isJson = publisher.isJson(),
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

fun checkOutput(group : GroupContext, outputDir: String, expectedCount: Int, chained : Boolean) {
    val consumer = makeConsumer(group, outputDir, false)
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
    val verifyEncryptions = VerifyEncryptedBallots(group, record.manifest(),
        ElGamalPublicKey(record.jointPublicKey()!!),
        record.extendedBaseHash()!!,
        record.config(), 1)

    val stats = Stats()
    val ballotResult = verifyEncryptions.verifyBallots(record.encryptedAllBallots { true }, stats)
    println("verifyBallots =  $ballotResult")
    assertTrue( ballotResult is Ok)
    assertEquals( expectedCount, stats.count())

    if (chained) {
        val result = verifyEncryptions.verifyConfirmationChain(record)
        if (result is Err) {
            println("FAIL $result")
        }
        println("verifyConfirmationChain =  $ballotResult")
        assertTrue(result is Ok)
    }
}