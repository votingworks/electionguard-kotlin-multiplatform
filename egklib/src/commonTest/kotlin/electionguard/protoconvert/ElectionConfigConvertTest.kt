package electionguard.protoconvert

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.fileReadBytes
import electionguard.core.productionGroup
import electionguard.input.buildStandardManifest
import electionguard.publish.Publisher
import electionguard.publish.electionRecordFromConsumer
import electionguard.publish.makeConsumer
import electionguard.publish.makePublisher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Can use this to generate a new ElectionConfig as needed. */
private const val ncontests = 20
private const val nselections = 5

class ElectionConfigConvertTest {
    val outputDir = "testOut/ElectionConfigConvertTest"
    val publisher = makePublisher(outputDir, true)

    @Test
    fun roundtripElectionConfig() {
        val electionConfig = generateElectionConfig(publisher, 6, 4)
        val proto = electionConfig.publishProto()
        val roundtrip = proto.import().getOrThrow { IllegalStateException(it) }
        assertNotNull(roundtrip)
        assertEquals(roundtrip.constants, electionConfig.constants)
        assertEquals(roundtrip.manifest, electionConfig.manifest)
        assertEquals(roundtrip.numberOfGuardians, electionConfig.numberOfGuardians)
        assertEquals(roundtrip.quorum, electionConfig.quorum)
        assertEquals(roundtrip.metadata, electionConfig.metadata)

        assertTrue(roundtrip.equals(electionConfig))
        assertEquals(roundtrip, electionConfig)

        publisher.writeElectionConfig(electionConfig)
        println("Wrote to $outputDir")

        val electionRecord = electionRecordFromConsumer(makeConsumer(outputDir, productionGroup()))
        validateElectionConfigParams(electionRecord.config())
    }
}

fun validateElectionConfigParams(config : ElectionConfig) {
    val Hp = parameterBaseHash(config.constants)
    assertEquals(Hp, config.parameterBaseHash)
    val Hm = manifestHash(Hp, config.manifestFile)
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

fun generateElectionConfig(publisher: Publisher, nguardians: Int, quorum: Int): ElectionConfig {
    // write out a manifest
    val fakeManifest = buildStandardManifest(ncontests, nselections)
    val filename = publisher.writeManifest(fakeManifest)

    // get it back as a file, to store in the ElectionConfig
    val manifestBytes = fileReadBytes(filename)

    return ElectionConfig(
        protocolVersion,
        productionGroup().constants,
        manifestBytes,
        fakeManifest,
        nguardians,
        quorum,
        "date",
        "juris",
    )
}