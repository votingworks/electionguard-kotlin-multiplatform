package electionguard.publish

import com.github.michaelbull.result.*
import electionguard.ballot.ElectionInitialized
import electionguard.core.productionGroup
import electionguard.input.ManifestInputValidation
import electionguard.protoconvert.generateElectionConfig
import electionguard.protoconvert.generateElementModP
import electionguard.protoconvert.generateGuardian
import electionguard.protoconvert.generateUInt256
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PublisherJsonTest {
    private val input = "src/commonTest/data/runWorkflowSomeAvailable"
    private val output = "testOut/PublisherJsonTest"

    val group = productionGroup()
    val consumerIn = makeConsumer(input, group)

    @Test
    fun testRoundtripElectionConfig() {
        val output1 = output + "1"
        val publisher = makePublisher(output1, true, true)
        val consumerOut = makeConsumer(output1, group, true)

        val config = generateElectionConfig(3, 3)

        // ManifestInputValidation
        val manifestValidator = ManifestInputValidation(config.manifest)
        val errors = manifestValidator.validate()
        if (errors.hasErrors()) {
            println("*** ManifestInputValidation FAILED on generated electionConfig")
            println("$errors")
            return
        }

        publisher.writeElectionConfig(config)

        val roundtripResult = consumerOut.readElectionConfig()
        assertNotNull(roundtripResult)
        if (roundtripResult is Err) {
            println("readElectionConfig = $roundtripResult")
        }
        assertTrue(roundtripResult is Ok)
        val roundtrip = roundtripResult.unwrap()

        assertEquals(config.constants, roundtrip.constants)
        assertEquals(config.manifest, roundtrip.manifest)
        // no way to store nguardians, quorum in json, so cant compare config
        // assertEquals(config, roundtrip)
    }

    @Test
    fun testRoundtripElectionInit() {
        val output2 = output + "2"
        val publisher = makePublisher(output2, true, true)
        val consumerOut = makeConsumer(output2, group, true)

        val config = generateElectionConfig(6, 4)
        publisher.writeElectionConfig(config)

        val init = ElectionInitialized(
            config,
            generateElementModP(group),
            config.manifest.cryptoHash,
            generateUInt256(group),
            generateUInt256(group),
            List(6) { generateGuardian(it, group) },
        )
        publisher.writeElectionInitialized(init)

        val roundtripResult = consumerOut.readElectionInitialized()
        assertNotNull(roundtripResult)
        if (roundtripResult is Err) {
            println("readElectionInitialized = $roundtripResult")
        }
        assertTrue(roundtripResult is Ok)
        val roundtrip = roundtripResult.unwrap()

        assertEquals(init.config.constants, roundtrip.config.constants)
        assertEquals(init.config.manifest, roundtrip.config.manifest)
        assertEquals(init.config.numberOfGuardians, roundtrip.config.numberOfGuardians)
        assertEquals(init.config.quorum, roundtrip.config.quorum)
        assertEquals(init.config, roundtrip.config)

        assertEquals(init.jointPublicKey, roundtrip.jointPublicKey)
        assertEquals(init.cryptoBaseHash, roundtrip.cryptoBaseHash)
        assertEquals(init.cryptoExtendedBaseHash, roundtrip.cryptoExtendedBaseHash)
        assertEquals(init.guardians, roundtrip.guardians)
        assertEquals(init, roundtrip)
    }

    @Test
    fun testWriteEncryptions() {
        val output3 = output + "3"
        val publisher = makePublisher(output3, true, true)
        val consumerOut = makeConsumer(output3, group, true)

        val initResult = consumerIn.readElectionInitialized()
        assertTrue(initResult is Ok)
        val init = initResult.unwrap()

        publisher.writeEncryptions(init, consumerIn.iterateEncryptedBallots { true })

        val rtResult = consumerOut.readElectionInitialized()
        if (rtResult is Err) {
            println("testWriteEncryptions = $rtResult")
        }
        assertTrue(rtResult is Ok)
        val roundtrip = rtResult.unwrap()

        assertEquals(init.config.constants, roundtrip.config.constants)
        assertEquals(init.config.manifest, roundtrip.config.manifest)
        assertEquals(init.config.numberOfGuardians, roundtrip.config.numberOfGuardians)
        assertEquals(init.config.quorum, roundtrip.config.quorum)
        // cant store metadata in json, so init not equal

        assertEquals(init.jointPublicKey, roundtrip.jointPublicKey)
        assertEquals(init.cryptoBaseHash, roundtrip.cryptoBaseHash)
        assertEquals(init.cryptoExtendedBaseHash, roundtrip.cryptoExtendedBaseHash)
        assertEquals(init.guardians, roundtrip.guardians)

        val inBallots = consumerIn.iterateEncryptedBallots { true }.associateBy { it.ballotId }
        consumerOut.iterateEncryptedBallots { true }.forEach {
            val inBallot = inBallots[it.ballotId] ?: throw RuntimeException("Cant find ${it.ballotId}")
            assertEquals(it, inBallot)
            println(" Ballot ${it.ballotId} OK")
        }
    }

    @Test
    fun testWriteSpoiledBallots() {
        val output4 = output + "4"
        val publisher = makePublisher(output4, true, true)
        val consumerOut = makeConsumer(output4, group, true)

        val sink: DecryptedTallyOrBallotSinkIF = publisher.decryptedTallyOrBallotSink()
        consumerIn.iterateDecryptedBallots().forEach {
            sink.writeDecryptedTallyOrBallot(it)
        }
        sink.close()

        val inBallots = consumerIn.iterateDecryptedBallots().associateBy { it.id }
        consumerOut.iterateDecryptedBallots().forEach {
            val inBallot = inBallots[it.id] ?: throw RuntimeException("Cant find ${it.id}")
            it.contests.forEach {
                val inContest = inBallot.contests.find { c -> it.contestId == c.contestId }
                    ?: throw RuntimeException("Cant find ${it.contestId}")
                it.selections.forEach {
                    val inSelection = inContest.selections.find { s -> it.selectionId == s.selectionId }
                        ?: throw RuntimeException("Cant find ${it.selectionId}")
                    assertEquals(inSelection.selectionId, it.selectionId)
                    assertEquals(inSelection.tally, it.tally)
                    assertEquals(inSelection.value, it.value)
                    assertEquals(inSelection.message, it.message)
                    assertEquals(inSelection.proof, it.proof)
                    assertEquals(inSelection, it)
                }
                // decryptedContestData not yet done
            }
            println(" Spoiled Ballot ${it.id} OK")
        }
    }


}