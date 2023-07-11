package electionguard.json2

import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionConstants
import electionguard.core.Base16.fromHex
import electionguard.core.Base16.toHex
import kotlinx.serialization.Serializable

@Serializable
data class ElectionConfigJson(
    val config_version: String,
    val number_of_guardians: Int,
    val quorum: Int,
    val election_date: String,
    val jurisdiction_info: String,

    val parameter_base_hash: UInt256Json, // Hp
    val manifest_hash: UInt256Json, // Hm
    val election_base_hash: UInt256Json, // Hb

    val chain_confirmation_codes: Boolean,
    val baux0: ByteArray, // B_aux,0 from eq 59,60
    val metadata: Map<String, String> = emptyMap(), // arbitrary key, value pairs
)

fun ElectionConfig.publishJson() = ElectionConfigJson(
    this.configVersion,
    this.numberOfGuardians,
    this.quorum,
    this.electionDate,
    this.jurisdictionInfo,
    this.parameterBaseHash.publishJson(),
    this.manifestHash.publishJson(),
    this.electionBaseHash.publishJson(),
    this.chainConfirmationCodes,
    this.baux0,
    this.metadata,
)

fun ElectionConfigJson.import(constants: ElectionConstants, manifestBytes: ByteArray) : ElectionConfig {
    return ElectionConfig(
        this.config_version,
        constants,
        this.number_of_guardians,
        this.quorum,
        this.election_date,
        this.jurisdiction_info,
        this.parameter_base_hash.import(),
        this.manifest_hash.import(),
        this.election_base_hash.import(),
        manifestBytes,
        this.chain_confirmation_codes,
        this.baux0,
        this.metadata,
    )
}

@Serializable
data class ElectionConstantsJson(
    val name: String,
    val large_prime: String,
    val small_prime: String,
    val cofactor: String,
    val generator: String,
)

fun ElectionConstants.publishJson() = ElectionConstantsJson(
    this.name,
    this.largePrime.toHex(),
    this.smallPrime.toHex(),
    this.cofactor.toHex(),
    this.generator.toHex(),
)

fun ElectionConstantsJson.import() = ElectionConstants(
    this.name,
    this.large_prime.fromHex()?: throw RuntimeException(),
    this.small_prime.fromHex()?: throw RuntimeException(),
    this.cofactor.fromHex()?: throw RuntimeException(),
    this.generator.fromHex()?: throw RuntimeException(),
)
