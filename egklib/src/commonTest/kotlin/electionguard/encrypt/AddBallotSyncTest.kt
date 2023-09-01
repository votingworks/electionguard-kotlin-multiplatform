package electionguard.encrypt

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedBallot
import electionguard.core.productionGroup
import electionguard.input.RandomBallotProvider
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class AddBallotSyncTest {
    val group = productionGroup()
    val inputProto = "src/commonTest/data/workflow/allAvailableProto"
    val inputJson = "src/commonTest/data/workflow/allAvailableJson"
    val outputDirTop = "testOut/encrypt/AddBallotSyncTest"

    val nballots = 4

    @Test
    fun testJsonSyncNoChain() {
        val outputDir = "$outputDirTop/testJsonSyncNoChain"
        val device = "device1"

        val group = productionGroup()
        val electionRecord = readElectionRecord(group, inputJson)
        val electionInit = electionRecord.electionInit()!!
        val publisher = makePublisher(outputDir, true, true)
        publisher.writeElectionInitialized(electionInit)

        val encryptor = AddEncryptedBallot(
            group,
            electionRecord.manifest(),
            electionInit,
            device,
            outputDir,
            "outputDir/invalidDir",
            true,
            false,
        )
        val ballotProvider = RandomBallotProvider(electionRecord.manifest())

        repeat(3) {
            repeat(nballots) {
                val ballot = ballotProvider.makeBallot()
                val result = encryptor.encrypt(ballot)
                assertTrue(result is Ok)
                encryptor.submit(result.unwrap().confirmationCode, EncryptedBallot.BallotState.CAST)
            }
            encryptor.sync()
        }
        encryptor.close()

        checkOutput(group, outputDir, 3 * nballots, false)

        // should fail once closed
        val ballot = ballotProvider.makeBallot()
        val result = encryptor.encrypt(ballot)
        assertTrue(result is Err)
        assertEquals("Adding ballot after chain has been closed", result.error)
    }

    @Test
    fun testProtoSyncNoChain() {
        val outputDir = "$outputDirTop/testProtoSyncNoChain"
        val device = "device1"

        val group = productionGroup()
        val electionRecord = readElectionRecord(group, inputProto)
        val electionInit = electionRecord.electionInit()!!
        val publisher = makePublisher(outputDir, true, false)
        publisher.writeElectionInitialized(electionInit)

        val encryptor = AddEncryptedBallot(
            group,
            electionRecord.manifest(),
            electionInit,
            device,
            outputDir,
            "outputDir/invalidDir",
            false,
            false,
        )
        val ballotProvider = RandomBallotProvider(electionRecord.manifest())

        repeat(3) {
            repeat(nballots) {
                val ballot = ballotProvider.makeBallot()
                val result = encryptor.encrypt(ballot)
                assertTrue(result is Ok)
                encryptor.submit(result.unwrap().confirmationCode, EncryptedBallot.BallotState.CAST)
            }
            encryptor.sync()
        }
        encryptor.close()

        checkOutput(group, outputDir, 3 * nballots, false)

        // should fail once closed
        val ballot = ballotProvider.makeBallot()
        val result = encryptor.encrypt(ballot)
        assertTrue(result is Err)
        assertEquals("Adding ballot after chain has been closed", result.error)
    }

    @Test
    fun testJsonSyncChain() {
        val outputDir = "$outputDirTop/testJsonSyncChain"
        val device = "device1"

        val group = productionGroup()
        val electionRecord = readElectionRecord(group, inputJson)
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
            "outputDir/invalidDir",
            true,
            false,
        )
        val ballotProvider = RandomBallotProvider(electionRecord.manifest())

        repeat(3) {
            repeat(nballots) {
                val ballot = ballotProvider.makeBallot()
                val result = encryptor.encrypt(ballot)
                assertTrue(result is Ok)
                encryptor.submit(result.unwrap().confirmationCode, EncryptedBallot.BallotState.CAST)
            }
            encryptor.sync()
        }
        encryptor.close()

        checkOutput(group, outputDir, 3 * nballots, true)

        // should fail once closed
        val ballot = ballotProvider.makeBallot()
        val result = encryptor.encrypt(ballot)
        assertTrue(result is Err)
        assertEquals("Adding ballot after chain has been closed", result.error)
    }

    @Test
    fun testProtoSyncChain() {
        val outputDir = "$outputDirTop/testProtoSyncChain"
        val device = "device1"

        val group = productionGroup()
        val electionRecord = readElectionRecord(group, inputProto)
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
            "outputDir/invalidDir",
            false,
            false,
        )
        val ballotProvider = RandomBallotProvider(electionRecord.manifest())

        repeat(3) {
            repeat(nballots) {
                val ballot = ballotProvider.makeBallot()
                val result = encryptor.encrypt(ballot)
                assertTrue(result is Ok)
                encryptor.submit(result.unwrap().confirmationCode, EncryptedBallot.BallotState.CAST)
            }
            encryptor.sync()
        }
        encryptor.close()

        checkOutput(group, outputDir, 3 * nballots, true)

        // should fail once closed
        val ballot = ballotProvider.makeBallot()
        val result = encryptor.encrypt(ballot)
        assertTrue(result is Err)
        assertEquals("Adding ballot after chain has been closed", result.error)
    }

}