package electionguard.testvectors

import electionguard.ballot.*
import electionguard.core.Base16.fromHex
import electionguard.core.Base16.toHex
import electionguard.core.UInt256
import electionguard.core.hashFunction
import electionguard.core.productionGroup
import electionguard.json2.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.junit.jupiter.api.io.TempDir
import java.io.FileOutputStream
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.use
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.text.toByteArray

class ParametersTestVector {
    private val jsonFormat = Json { prettyPrint = true }
    private var outputFile = "testOut/testvectors/ParametersTestVector.json"

    @Serializable
    data class PrimesJson(
        val large_prime: String,
        val small_prime: String,
        val generator: String,
    )

    @Serializable
    data class ParameterBaseHash(
        val task : String,
        val protocolVersion : String,
        val primes : PrimesJson,
        val expected : UInt256Json,
    )

    @Serializable
    data class ManifestHash(
        val task : String,
        val parameterBaseHash : UInt256Json,
        val manifest : String,
        val expected : UInt256Json,
    )

    @Serializable
    data class ElectionBaseHash(
        val task : String,
        val parameterBaseHash : UInt256Json,
        val manifest_hash : UInt256Json,
        val numberOfGuardians: Int,
        val quorum: Int,
        val expected : UInt256Json,
    )

    @Serializable
    data class ParametersTestVector(
        val desc: String,
        val parameter_base_hash : ParameterBaseHash,
        val manifest_hash : ManifestHash,
        val election_base_hash : ElectionBaseHash,
    )

    @Test
    fun testParametersTestVector(@TempDir tempDir : Path) {
        outputFile = tempDir.resolve("ParametersTestVector.json").toString()
        makeParametersTestVector()
        readParametersTestVector()
    }

    fun makeParametersTestVector() {
        val constants = productionGroup().constants
        val primesJson = PrimesJson(constants.largePrime.toHex(), constants.smallPrime.toHex(), constants.generator.toHex(), )
        val expectedParameterBaseHash = parameterBaseHash(constants)
        val parameterBaseHash = ParameterBaseHash(
            "Generate parameter base hash. spec 2.0.0, eq 4",
            protocolVersion,
            primesJson,
            expectedParameterBaseHash.publishJson())


        val fakeManifest = "your manifest goes here"
        val expectedManifestHash = manifestHash(expectedParameterBaseHash, fakeManifest.toByteArray())
        val manifestTestVector = ManifestHash(
            "Generate manifest hash. spec 2.0.0, eq 6",
            expectedParameterBaseHash.publishJson(),
            fakeManifest,
            expectedManifestHash.publishJson(),
        )

        val n = 6
        val k = 4
        val electionBaseHash = ElectionBaseHash(
            "Generate election base hash. spec 2.0.0, eq 7",
            expectedParameterBaseHash.publishJson(),
            expectedManifestHash.publishJson(),
            n, k,
            electionBaseHash(expectedParameterBaseHash, expectedManifestHash, n, k).publishJson(),
        )

        val parametersTestVector = ParametersTestVector(
            "Test hash function, generation of Hp, Hm, and Hb",
            parameterBaseHash,
            manifestTestVector,
            electionBaseHash
        )
        println(jsonFormat.encodeToString(parametersTestVector))

        FileOutputStream(outputFile).use { out ->
            jsonFormat.encodeToStream(parametersTestVector, out)
            out.close()
        }
    }

    fun readParametersTestVector() {
        val fileSystem = FileSystems.getDefault()
        val fileSystemProvider = fileSystem.provider()
        val parametersTestVector : ParametersTestVector =
            fileSystemProvider.newInputStream(fileSystem.getPath(outputFile)).use { inp ->
                Json.decodeFromStream<ParametersTestVector>(inp)
            }

        val parameterV = parametersTestVector.parameter_base_hash
        val version = parameterV.protocolVersion.toByteArray()
        val HV = ByteArray(32) { if (it < version.size) version[it] else 0 }
        val actualHp = hashFunction(
            HV,
            0x00.toByte(),
            parameterV.primes.large_prime.fromHex()!!,
            parameterV.primes.small_prime.fromHex()!!,
            parameterV.primes.generator.fromHex()!!,
        )
        assertEquals(parameterV.expected.import(), actualHp)

        val electionM = parametersTestVector.manifest_hash
        val actualM = hashFunction(
            electionM.parameterBaseHash.import().bytes,
            0x01.toByte(),
            electionM.manifest.length,
            electionM.manifest.toByteArray(),
        )
        assertEquals(electionM.expected.import(), actualM)
        val actualM2 = manifestHash(
            electionM.parameterBaseHash.import(),
            electionM.manifest.toByteArray(),
        )
        assertEquals(electionM.expected.import(), actualM2)

        val electionV = parametersTestVector.election_base_hash
        val actualHb = hashFunction(
            electionV.parameterBaseHash.import().bytes,
            0x02.toByte(),
            electionV.manifest_hash.import().bytes,
            electionV.numberOfGuardians,
            electionV.quorum,
        )
        assertEquals(electionV.expected.import(), actualHb)
        val actualHb2 = electionBaseHash(
            electionV.parameterBaseHash.import(),
            electionV.manifest_hash.import(),
            electionV.numberOfGuardians,
            electionV.quorum,
        )
        assertEquals(electionV.expected.import(), actualHb2)
    }

}