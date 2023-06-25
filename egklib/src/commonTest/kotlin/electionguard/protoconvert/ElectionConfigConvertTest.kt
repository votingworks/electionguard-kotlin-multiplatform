package electionguard.protoconvert

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.fileReadBytes
import electionguard.core.productionGroup
import electionguard.input.buildTestManifest
import electionguard.publish.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Can use this to generate a new ElectionConfig as needed. */
private const val ncontests = 20
private const val nselections = 5

class ElectionConfigConvertTest {
    val outputDir = "testOut/protoconvert/ElectionConfigConvertTestJson"
    val publisher = makePublisher(outputDir, true, true)

    @Test
    fun roundtripElectionConfig() {
        val (manifest, electionConfig) = generateElectionConfig(publisher, 6, 4)
        val proto = electionConfig.publishProto()
        val roundtrip = proto.import().getOrThrow { IllegalStateException(it) }
        assertNotNull(roundtrip)
        assertEquals(roundtrip.constants, electionConfig.constants)
        assertEquals(roundtrip.numberOfGuardians, electionConfig.numberOfGuardians)
        assertEquals(roundtrip.quorum, electionConfig.quorum)
        assertEquals(roundtrip.metadata, electionConfig.metadata)

        assertTrue(roundtrip.equals(electionConfig))
        assertEquals(roundtrip, electionConfig)

        publisher.writeElectionConfig(electionConfig)
        println("Wrote to $outputDir")

        val electionRecord = readElectionRecord(productionGroup(), outputDir)
        val config = electionRecord.config()

        val Hp = parameterBaseHash(config.constants)
        assertEquals(Hp, config.parameterBaseHash)
        val Hm = manifestHash(Hp, electionRecord.manifestBytes())
        assertEquals(Hm, config.manifestHash)
        assertEquals(
            electionBaseHash(
                Hp,
                config.numberOfGuardians,
                config.quorum,
                config.electionDate,
                config.jurisdictionInfo,
                Hm
            ), config.electionBaseHash
        )
    }
}

fun generateElectionConfig(publisher: Publisher, nguardians: Int, quorum: Int): Pair<Manifest, ElectionConfig> {
    // write out a manifest
    val fakeManifest = buildTestManifest(ncontests, nselections)
    val filename = publisher.writeManifest(fakeManifest)

    // get it back as a file, to store in the ElectionConfig
    val manifestBytes = fileReadBytes(filename)

    val config = makeElectionConfig(
        protocolVersion,
        productionGroup().constants,
        nguardians,
        quorum,
        "date",
        "juris",
        manifestBytes,
    )
    return Pair(fakeManifest, config)
}