package electionguard.protoconvert

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.fileReadBytes
import electionguard.core.productionGroup
import electionguard.input.buildTestManifest
import electionguard.publish.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Can use this to generate a new ElectionConfig as needed. */
private const val ncontests = 20
private const val nselections = 5

class ElectionConfigConvertTest {

    @Test
    fun roundtripElectionConfig() {
        val outputDir = "testOut/protoconvert/roundtripElectionConfig"
        val publisher = makePublisher(outputDir, true, true)

        val (_, electionConfig) = generateElectionConfig(publisher, 6, 4)
        val roundtrip = roundtripProtoPublishJson(electionConfig, outputDir, true)
        compareElectionConfig(electionConfig, roundtrip)
    }

    @Test
    fun roundtripWithChain() {
        val outputDir = "testOut/protoconvert/roundtripWithChain"
        // clear output dir
        val publisher = makePublisher(outputDir, true, false)
        // write out a manifest
        val fakeManifest = buildTestManifest(ncontests, nselections)
        val filename = publisher.writeManifest(fakeManifest)

        // get it back as a file, to store in the ElectionConfig
        val manifestBytes = fileReadBytes(filename)

        val electionConfig = makeElectionConfig(
            protocolVersion,
            productionGroup().constants,
            11,
            7,
            manifestBytes,
            true,
            "device".encodeToByteArray(),
            mapOf(
                Pair("Created by", "roundtripWithChain"),
                Pair("Created for", "testing"),
                Pair("Created on", "9/9/1999"),
                ),
            )

        val roundtrip = roundtripProtoPublishJson(electionConfig, outputDir, false)
        compareElectionConfig(electionConfig, roundtrip)
    }
}

fun roundtripProtoPublishJson(electionConfig: ElectionConfig, outputDir: String, isJson: Boolean): ElectionConfig {
    // roundtrip proto
    val proto = electionConfig.publishProto()
    val roundtrip = proto.import().getOrThrow { IllegalStateException(it) }
    compareElectionConfig(electionConfig, roundtrip)

    // publish json
    val publisher = makePublisher(outputDir, true, isJson)
    publisher.writeElectionConfig(roundtrip)

    val electionRecord = readElectionRecord(productionGroup(), outputDir)
    return electionRecord.config()
}

fun compareElectionConfig(expected: ElectionConfig, actual: ElectionConfig, ) {

    assertEquals(expected.constants, actual.constants)
    assertEquals(expected.numberOfGuardians, actual.numberOfGuardians)
    assertEquals(expected.quorum, actual.quorum)
    assertEquals(expected.metadata, actual.metadata)
    assertEquals(expected.chainConfirmationCodes, actual.chainConfirmationCodes)
    println("chainConfirmationCodes = ${actual.chainConfirmationCodes}")

    val Hp = parameterBaseHash(actual.constants)
    assertEquals(expected.parameterBaseHash, Hp)
    val Hm = manifestHash(Hp, actual.manifestBytes)
    assertEquals(expected.manifestHash, Hm)
    assertEquals(
        expected.electionBaseHash,
        electionBaseHash(
            Hp,
            Hm,
            actual.numberOfGuardians,
            actual.quorum,
        )
    )
    assertTrue(expected.equals(actual))
    assertEquals(expected, actual)
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
        manifestBytes,
        false,
        "device".encodeToByteArray(),
        mapOf(Pair("Created by", "generateElectionConfig")),
        )
    return Pair(fakeManifest, config)
}