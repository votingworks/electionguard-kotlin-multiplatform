package electionguard.encrypt

import electionguard.ballot.EncryptedBallot
import electionguard.core.*
import electionguard.input.RandomBallotProvider
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import electionguard.util.ErrorMessages
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNotNull

class AddEncryptedUnorderedTest {
    val group = productionGroup()
    val input = "src/commonTest/data/workflow/allAvailableJson"
    val outputDirProto = "testOut/encrypt/AddEncryptedUnorderedTest"

    val nballots = 3

    @Test
    fun testJustOneDevice() {
        val outputDir = "$outputDirProto/testJustOne"
        val device = "device0"

        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!
        val publisher = makePublisher(outputDir, true, true)
        publisher.writeElectionInitialized(electionInit)

        val encryptor = AddEncryptedBallot(
            group,
            electionRecord.manifest(),
            electionInit.config.chainConfirmationCodes,
            electionInit.config.configBaux0,
            electionInit.jointPublicKey(),
            electionInit.extendedBaseHash,
            device,
            outputDir,
            "${outputDir}/invalidDir",
            isJson = publisher.isJson(),
        )
        val ballotProvider = RandomBallotProvider(electionRecord.manifest())

        val cballots = mutableListOf<CiphertextBallot>()
        repeat(nballots) {
            val ballot = ballotProvider.makeBallot()
            val result = encryptor.encrypt(ballot, ErrorMessages("testJustOneDevice"))
            assertNotNull(result)
            cballots.add(result)
        }
        cballots.shuffle()
        cballots.forEach { encryptor.submit(it.confirmationCode, EncryptedBallot.BallotState.CAST) }
        encryptor.close()

        checkOutput(group, outputDir, nballots, false)
    }

    @Test
    fun testMultipleDevices() {
        val outputDir = "$outputDirProto/testMultipleDevices"

        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!
        val publisher = makePublisher(outputDir, true, true)
        publisher.writeElectionInitialized(electionInit)

        repeat(3) { it ->
            val encryptor = AddEncryptedBallot(
                group,
                electionRecord.manifest(),
                electionInit.config.chainConfirmationCodes,
                electionInit.config.configBaux0,
                electionInit.jointPublicKey(),
                electionInit.extendedBaseHash,
                "device$it",
                outputDir,
                "$outputDir/invalidDir",
                isJson = publisher.isJson(),
            )
            val ballotProvider = RandomBallotProvider(electionRecord.manifest())

            val cballots = mutableListOf<CiphertextBallot>()
            repeat(nballots) {
                val ballot = ballotProvider.makeBallot()
                val result = encryptor.encrypt(ballot, ErrorMessages("testMultipleDevices"))
                assertNotNull(result)
                cballots.add(result)
            }
            cballots.shuffle()
            cballots.forEach { encryptor.submit(it.confirmationCode, EncryptedBallot.BallotState.CAST) }
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

        val publisher = makePublisher(outputDir, true, true)
        publisher.writeElectionInitialized(electionInit)

        val encryptor = AddEncryptedBallot(
            group,
            electionRecord.manifest(),
            electionInit.config.chainConfirmationCodes,
            electionInit.config.configBaux0,
            electionInit.jointPublicKey(),
            electionInit.extendedBaseHash,
            device,
            outputDir,
            "${outputDir}/invalidDir",
            isJson = publisher.isJson(),
        )
        val ballotProvider = RandomBallotProvider(electionRecord.manifest())

        val cballots = mutableListOf<CiphertextBallot>()
        repeat(nballots) {
            val ballot = ballotProvider.makeBallot()
            val result = encryptor.encrypt(ballot, ErrorMessages("testOneWithChain"))
            assertNotNull(result)
            cballots.add(result)
        }
        cballots.shuffle()
        cballots.forEach { encryptor.submit(it.confirmationCode, EncryptedBallot.BallotState.CAST) }
        encryptor.close()

        checkOutput(group, outputDir, nballots, true)
    }

    @Test
    fun testMultipleDevicesChaining() {
        val outputDir = "$outputDirProto/testMultipleDevicesChaining"

        val electionRecord = readElectionRecord(group, input)
        val configWithChaining = electionRecord.config().copy(chainConfirmationCodes = true)
        val electionInit = electionRecord.electionInit()!!.copy(config = configWithChaining)

        val publisher = makePublisher(outputDir, true, true)
        publisher.writeElectionInitialized(electionInit)

        repeat(3) { it ->
            val encryptor = AddEncryptedBallot(
                group,
                electionRecord.manifest(),
                electionInit.config.chainConfirmationCodes,
                electionInit.config.configBaux0,
                electionInit.jointPublicKey(),
                electionInit.extendedBaseHash,
                "device$it",
                outputDir,
                "$outputDir/invalidDir",
                isJson = publisher.isJson(),
            )
            val ballotProvider = RandomBallotProvider(electionRecord.manifest())

            val cballots = mutableListOf<CiphertextBallot>()
            repeat(nballots) {
                val ballot = ballotProvider.makeBallot()
                val result = encryptor.encrypt(ballot, ErrorMessages("testMultipleDevicesChaining"))
                assertNotNull(result)
                cballots.add(result)
            }
            cballots.shuffle()
            cballots.forEach { encryptor.submit(it.confirmationCode, EncryptedBallot.BallotState.CAST) }
            encryptor.close()
        }

        checkOutput(group, outputDir, 3 * nballots, true)
    }

    // TODO concurrent modification exception
    @Test
    fun testCallMultipleTimes() {
        val outputDir = "$outputDirProto/testCallMultipleTimes"
        testMultipleCalls(outputDir, true, true, false)
        testMultipleCalls(outputDir, false, true, false)
        testMultipleCalls(outputDir, true, false, false)
        testMultipleCalls(outputDir, false, false, false)
        testMultipleCalls(outputDir, true, true, true)
        testMultipleCalls(outputDir, false, true, true)
        testMultipleCalls(outputDir, true, false, true)
        testMultipleCalls(outputDir, false, false, true)
    }

    /**
     * @param shuffle: shuffle the order in which the encrypted ballots are submitted
     * @param skip: some of the encrypted ballots are not submitted
     */
    fun testMultipleCalls(outputDir: String, shuffle: Boolean, skip: Boolean, chained: Boolean) {
        try {
            val device = "deviceM"

            val electionRecord = readElectionRecord(group, input)
            val electionInit = if (chained) {
                val configWithChaining = electionRecord.config().copy(chainConfirmationCodes = true)
                electionRecord.electionInit()!!.copy(config = configWithChaining)
            } else {
                electionRecord.electionInit()!!
            }
            val publisher = makePublisher(outputDir, true, true)
            publisher.writeElectionInitialized(electionInit)

            println("makePublisher makeNewOutput=$outputDir")
            println("shuffle=$shuffle skip=$skip chained=$chained")
            repeat(3) {
                val encryptor = AddEncryptedBallot(
                    group,
                    electionRecord.manifest(),
                    electionInit.config.chainConfirmationCodes,
                    electionInit.config.configBaux0,
                    electionInit.jointPublicKey(),
                    electionInit.extendedBaseHash,
                    device,
                    outputDir,
                    "${outputDir}/invalidDir",
                    isJson = publisher.isJson(),
                )
                val ballotProvider = RandomBallotProvider(electionRecord.manifest())

                val cballots = mutableListOf<CiphertextBallot>()
                repeat(nballots) {
                    val ballot = ballotProvider.makeBallot()
                    val result = encryptor.encrypt(ballot, ErrorMessages("testMultipleCalls"))
                    assertNotNull(result)
                    cballots.add(result)
                }
                if (shuffle) {
                    cballots.shuffle()
                }

                cballots.forEach {
                    val random = Random.nextInt(10)
                    val state = if (random < 6) EncryptedBallot.BallotState.SPOILED else EncryptedBallot.BallotState.CAST
                    val skipThisOne = skip && random < 2 // skip submitting 2 in 10
                    println(" skip $skipThisOne state $state")
                    if (!skipThisOne) {
                        encryptor.submit(it.confirmationCode, state)
                    }
                }
                encryptor.close()
            }

            checkOutput(group, outputDir, 3 * nballots, chained)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}