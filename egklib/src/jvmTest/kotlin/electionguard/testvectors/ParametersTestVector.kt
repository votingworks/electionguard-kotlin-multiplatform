package electionguard.testvectors

import electionguard.ballot.*
import electionguard.core.Base16.fromHex
import electionguard.core.Base16.toHex
import electionguard.core.UInt256
import electionguard.core.hashFunction
import electionguard.core.productionGroup
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.FileOutputStream
import java.nio.file.FileSystems
import kotlin.io.use
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.text.toByteArray

class ParametersTestVector {
    private val jsonFormat = Json { prettyPrint = true }
    private val outputFile = "testOut/testvectors/ParametersTestVector.json"

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
    data class ElectionBaseHash(
        val task : String,
        val parameterBaseHash : UInt256Json,
        val numberOfGuardians: Int,
        val quorum: Int,
        val date : String,
        val jurisdiction_info : String,
        val manifest_hash : UInt256Json,
        val expected : UInt256Json,
    )

    @Serializable
    data class ParametersTestVector(
        val parameter_base_hash : ParameterBaseHash,
        val election_base_hash : ElectionBaseHash,
    )

    @Test
    fun testParametersTestVector() {
        makeParametersTestVector()
        readParametersTestVector()
    }

    fun makeParametersTestVector() {
        val constants = productionGroup().constants
        val primesJson = PrimesJson(constants.largePrime.toHex(), constants.smallPrime.toHex(), constants.generator.toHex(), )
        val expectedParameterBaseHash = parameterBaseHash(constants)
        val parameterBaseHash = ParameterBaseHash(
            "Generate parameter base hash. spec 1.9, p 15, eq 4",
            protocolVersion,
            primesJson,
            expectedParameterBaseHash.publishJson())

        val n = 6
        val k = 4
        val date = "Nov 2, 1960"
        val info = "Los Angeles County, general election"
        val HM = UInt256.random()
        val electionBaseHash = ElectionBaseHash(
            "Generate election base hash. spec 1.9, p 17, eq 6",
            expectedParameterBaseHash.publishJson(),
            n, k, date, info, HM.publishJson(),
            // fun electionBaseHash(Hp: UInt256, n : Int, k : Int, date : String, info : String, HM: UInt256) : UInt256 {
            electionBaseHash(expectedParameterBaseHash, n, k, date, info, HM).publishJson(),
        )

        val parametersTestVector = ParametersTestVector(
            parameterBaseHash,
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
        val HV = ByteArray(32) { if (it < 4) version[it] else 0 }
        val actualHp = hashFunction(
            HV,
            0x00.toByte(),
            parameterV.primes.large_prime.fromHex()!!,
            parameterV.primes.small_prime.fromHex()!!,
            parameterV.primes.generator.fromHex()!!,
        )
        assertEquals(parameterV.expected.import(), actualHp)

        val electionV = parametersTestVector.election_base_hash
        val actualHb = electionBaseHash(
            electionV.parameterBaseHash.import(),
            electionV.numberOfGuardians,
            electionV.quorum,
            electionV.date,
            electionV.jurisdiction_info,
            electionV.manifest_hash.import(),
        )
        assertEquals(electionV.expected.import(), actualHb)
    }

}