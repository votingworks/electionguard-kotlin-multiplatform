package electionguard.protoconvert

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.productionGroup
import electionguard.input.buildStandardManifest
import electionguard.input.protoVersion
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Can use this to generate a new ElectionConfig as needed. */
private const val writeout = true
private const val ncontests = 20
private const val nselections = 5

class ElectionConfigConvertTest {

    @Test
    fun roundtripElectionConfig() {
        val electionConfig = generateElectionConfig(6, 4)
        val proto = electionConfig.publishElectionConfig()
        val roundtrip = importElectionConfig(proto).getOrThrow { IllegalStateException(it) }
        assertNotNull(roundtrip)
        assertEquals(roundtrip.protoVersion, electionConfig.protoVersion)
        assertEquals(roundtrip.constants, electionConfig.constants)
        assertEquals(roundtrip.manifest, electionConfig.manifest)
        assertEquals(roundtrip.numberOfGuardians, electionConfig.numberOfGuardians)
        assertEquals(roundtrip.quorum, electionConfig.quorum)
        assertEquals(roundtrip.metadata, electionConfig.metadata)

        assertTrue(roundtrip.equals(electionConfig))
        assertEquals(roundtrip, electionConfig)

        if (writeout) {
            val output = "testOut/ElectionConfigConvertTest"
            val publisher = Publisher(output, PublisherMode.createNew)
            publisher.writeElectionConfig(electionConfig)
            println("Wrote to $output")
        }
    }
}

fun generateElectionConfig(nguardians: Int, quorum: Int): ElectionConfig {
    return ElectionConfig(
        protoVersion,
        productionGroup().constants,
        buildStandardManifest(ncontests, nselections),
        nguardians, quorum
    )
}