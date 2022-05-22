package electionguard.publish

import com.github.michaelbull.result.*
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.protoconvert.generateElectionConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PublisherTest {
    private val input = "src/commonTest/data/runWorkflowAllAvailable"
    private val output = "testout/PublisherTest"
    val publisher = Publisher(output, PublisherMode.createNew)

    @Test
    fun testWriteElectionConfig() {
        val config = generateElectionConfig()
        publisher.writeElectionConfig(config)

        val context = productionGroup()
        val electionRecordIn = ElectionRecord(output, context)
        val roundtripResult = electionRecordIn.readElectionConfig()
        assertNotNull(roundtripResult)
        assertTrue(roundtripResult is Ok)
        val roundtrip = roundtripResult.unwrap()

        assertEquals(config.protoVersion, roundtrip.protoVersion)
        assertEquals(config.constants, roundtrip.constants)
        assertEquals(config.manifest, roundtrip.manifest)
        assertEquals(config.numberOfGuardians, roundtrip.numberOfGuardians)
        assertEquals(config.quorum, roundtrip.quorum)
        assertEquals(config.metadata, roundtrip.metadata)

        assertTrue(roundtrip.equals(config))
        assertEquals(config, roundtrip)
    }

    @Test
    fun testWriteEncryptions() {
        runTest {
            val context = productionGroup()
            val electionRecordIn = ElectionRecord(input, context)
            val initResult = electionRecordIn.readElectionInitialized()
            assertTrue(initResult is Ok)
            val init = initResult.unwrap()

            publisher.writeEncryptions(init, electionRecordIn.iterateEncryptedBallots { true })

            val electionRecordRoundtrip = ElectionRecord(output, context)
            val rtResult = electionRecordRoundtrip.readElectionInitialized()
            assertTrue(rtResult is Ok)
            val rtInit = rtResult.unwrap()
            assertTrue(rtInit.equals(init))
            assertEquals(init, rtInit)

        }
    }



}