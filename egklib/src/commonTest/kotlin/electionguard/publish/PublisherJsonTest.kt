package electionguard.publish

import com.github.michaelbull.result.*
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.EncryptedBallot
import electionguard.core.*
import electionguard.input.ManifestInputValidation
import electionguard.protoconvert.generateElectionConfig
import electionguard.protoconvert.generateGuardian
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PublisherJsonTest {
    private val input = "src/commonTest/data/workflow/someAvailableProto"
    private val output = "testOut/publish/PublisherJsonTest"

    val group = productionGroup()
    val consumerIn = makeConsumer(group, input)

    @Test
    fun testRoundtripElectionConfig() {
        val output1 = output + "1"
        val publisher = makePublisher(output1, true, true)
        val consumerOut = makeConsumer(group, output1, true)

        val (manifest, config) = generateElectionConfig(publisher, 3, 3)

        // ManifestInputValidation
        val manifestValidator = ManifestInputValidation(manifest)
        val errors = manifestValidator.validate()
        if (errors.hasErrors()) {
            println("*** ManifestInputValidation error on generated electionConfig")
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
        // assertEquals(config.manifest, roundtrip.manifest) TODO manifest
        // no way to store nguardians, quorum in json, so cant compare config
        // assertEquals(config, roundtrip)
    }

    @Test
    fun testRoundtripElectionInit() {
        val output2 = output + "2"
        val publisher = makePublisher(output2, true, true)
        val consumerOut = makeConsumer(group, output2, true)

        val (_, config) = generateElectionConfig(publisher, 6, 4)
        publisher.writeElectionConfig(config)

        val init = ElectionInitialized(
            config,
            generateElementModP(group),
            generateUInt256(group),
            List(6) { generateGuardian(it, group) },
        )
        publisher.writeElectionInitialized(init)

        val roundtripResult = consumerOut.readElectionInitialized()
        assertNotNull(roundtripResult)
        if (roundtripResult is Err) {
            println("readElectionInitialized = $roundtripResult")
        } else {
            println("readElectionInitialized Ok")
        }
        assertTrue(roundtripResult is Ok)
        val roundtrip = roundtripResult.unwrap()

        assertTrue(roundtrip.approxEquals(init))
    }

    @Test
    fun testWriteEncryptions() {
        val output3 = output + "3"
        val publisher = makePublisher(output3, true, true)
        val consumerOut = makeConsumer(group, output3, true)

        val initResult = consumerIn.readElectionInitialized()
        assertTrue(initResult is Ok)
        val init = initResult.unwrap()

        publisher.writeElectionInitialized(init)
        val esink = publisher.encryptedBallotSink("testWriteEncryptions")
        try {
            consumerIn.iterateAllEncryptedBallots { true }.forEach() {
                esink.writeEncryptedBallot(it)
            }
        } finally {
            esink.close()
        }

        val rtResult = consumerOut.readElectionInitialized()
        if (rtResult is Err) {
            println("testWriteEncryptions = $rtResult")
        }
        assertTrue(rtResult is Ok)
        val roundtrip = rtResult.unwrap()

        assertTrue(roundtrip.approxEquals(init))

        assertTrue(consumerOut.iterateAllEncryptedBallots { true }.approxEqualsEncryptedBallots(consumerIn.iterateAllEncryptedBallots { true }))
    }

    @Test
    fun testWriteSpoiledBallots() {
        val output4 = output + "4"
        val publisher = makePublisher(output4, true, true)
        val consumerOut = makeConsumer(group, output4, true)

        val dsink = publisher.decryptedTallyOrBallotSink()
        try {
            consumerIn.iterateDecryptedBallots().forEach() {
                dsink.writeDecryptedTallyOrBallot(it)
            }
        } finally {
            dsink.close()
        }

        assertTrue(consumerOut.iterateDecryptedBallots().approxEqualsDecryptedBallots(consumerIn.iterateDecryptedBallots()))
    }

    @Test
    fun testWriteTallyResults() {
        val output5 = output + "5"
        val publisher = makePublisher(output5, true, true)
        val consumerOut = makeConsumer(group, output5, true)

        val tallyResult = consumerIn.readTallyResult()
        if (tallyResult is Err) {
            println("readTallyResult from in = $tallyResult")
        }
        assertTrue(tallyResult is Ok)
        val tally = tallyResult.unwrap()

        publisher.writeTallyResult(tally)
        val rtResult = consumerOut.readTallyResult()
        if (rtResult is Err) {
            println("readTallyResult from out = $rtResult")
        }
        assertTrue(rtResult is Ok)
        val roundtrip = rtResult.unwrap()

        assertEquals(tally.encryptedTally, roundtrip.encryptedTally)
    }

    @Test
    fun testWriteDecryptionResults() {
        val output6 = output + "6"
        val publisher = makePublisher(output6, true, true)
        val consumerOut = makeConsumer(group, output6, true)

        val tallyResult = consumerIn.readDecryptionResult()
        if (tallyResult is Err) {
            println("readTallyResult from in = $tallyResult")
        }
        assertTrue(tallyResult is Ok)
        val tally = tallyResult.unwrap()

        publisher.writeDecryptionResult(tally)
        val rtResult = consumerOut.readDecryptionResult()
        if (rtResult is Err) {
            println("readTallyResult from out = $rtResult")
        }
        assertTrue(rtResult is Ok)
        val roundtrip = rtResult.unwrap()

        assertEquals(tally.decryptedTally, roundtrip.decryptedTally)
    }
}


// cant store metadata in json, so init not equal
fun ElectionInitialized.approxEquals(expected: ElectionInitialized) : Boolean {
    assertEquals(expected.config.constants, this.config.constants)
    // assertEquals(expected.config.manifest, this.config.manifest) TODO manifest
    assertEquals(expected.config.numberOfGuardians, this.config.numberOfGuardians)
    assertEquals(expected.config.quorum, this.config.quorum)

    assertEquals(expected.jointPublicKey, this.jointPublicKey)
    assertEquals(expected.extendedBaseHash, this.extendedBaseHash)
    assertEquals(expected.guardians, this.guardians)
    return true
}

fun Iterable<EncryptedBallot>.approxEqualsEncryptedBallots(expected: Iterable<EncryptedBallot>) : Boolean {
    val inBallots = expected.associateBy { it.ballotId }
    this.forEach {
        val inBallot = inBallots[it.ballotId] ?: throw RuntimeException("Cant find ${it.ballotId}")
        assertEquals(it, inBallot)
        println(" Ballot ${it.ballotId} OK")
    }
    return true
}

fun Iterable<DecryptedTallyOrBallot>.approxEqualsDecryptedBallots(expected: Iterable<DecryptedTallyOrBallot>) : Boolean {
    val inBallots = expected.associateBy { it.id }
    this.forEach {
        val inBallot = inBallots[it.id] ?: throw RuntimeException("Cant find ${it.id}")
        it.contests.forEach {
            val inContest = inBallot.contests.find { c -> it.contestId == c.contestId }
                ?: throw RuntimeException("Cant find ${it.contestId}")
            it.selections.forEach {
                val inSelection = inContest.selections.find { s -> it.selectionId == s.selectionId }
                    ?: throw RuntimeException("Cant find ${it.selectionId}")
                assertEquals(inSelection.selectionId, it.selectionId)
                assertEquals(inSelection.tally, it.tally)
                assertEquals(inSelection.bOverM, it.bOverM)
                assertEquals(inSelection.encryptedVote, it.encryptedVote)
                assertEquals(inSelection.proof, it.proof)
                assertEquals(inSelection, it)
            }
            // decryptedContestData not yet done
        }
        println(" Spoiled Ballot ${it.id} OK")
    }
    return true
}