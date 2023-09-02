package electionguard.encrypt

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedBallot
import electionguard.core.*
import electionguard.input.RandomBallotProvider
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import kotlin.test.Test
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
        val publisher = makePublisher(outputDir, true, true)
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

        repeat (nballots) {
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
        val outputDir = "$outputDirJson/testCallMultipleTimes"
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
        val outputDir = "$outputDirJson/testMultipleDevices"

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
        val outputDir = "$outputDirJson/testOneWithChain"
        val device = "device0"

        val group = productionGroup()
        val electionRecord = readElectionRecord(group, input)
        val configWithChaining = electionRecord.config().copy(chainConfirmationCodes = true)
        val electionInit = electionRecord.electionInit()!!.copy(config = configWithChaining)

        val publisher = makePublisher(outputDir, true, true)
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
        val outputDir = "$outputDirJson/testCallMultipleTimesChaining"
        val device = "device1"

        val group = productionGroup()
        val electionRecord = readElectionRecord(group, input)
        val configWithChaining = electionRecord.config().copy(chainConfirmationCodes = true)
        val electionInit = electionRecord.electionInit()!!.copy(config = configWithChaining)

        val publisher = makePublisher(outputDir, true, true)
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

        checkOutput(group, outputDir, 3 * nballots, true)
    }

    @Test
    fun testMultipleDevicesChaining() {
        val outputDir = "$outputDirJson/testMultipleDevicesChaining"

        val group = productionGroup()
        val electionRecord = readElectionRecord(group, input)
        val configWithChaining = electionRecord.config().copy(chainConfirmationCodes = true)
        val electionInit = electionRecord.electionInit()!!.copy(config = configWithChaining)

        val publisher = makePublisher(outputDir, true, true)
        publisher.writeElectionInitialized(electionInit)

        repeat(3) {
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