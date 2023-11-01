package electionguard.json2

import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionConstants
import electionguard.core.Base16.fromHex
import electionguard.core.Base16.toHex
import electionguard.util.ErrorMessages
import kotlinx.serialization.Serializable

@Serializable
data class ElectionConfigJson(
    val config_version: String,
    val number_of_guardians: Int,
    val quorum: Int,

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
    this.parameterBaseHash.publishJson(),
    this.manifestHash.publishJson(),
    this.electionBaseHash.publishJson(),
    this.chainConfirmationCodes,
    this.configBaux0,
    this.metadata,
)

fun ElectionConfigJson.import(constants: ElectionConstants?, manifestBytes: ByteArray?, errs:ErrorMessages) : ElectionConfig? {
    if (this.parameter_base_hash.import() == null) errs.add("malformed parameter_base_hash")
    if (this.manifest_hash.import() == null) errs.add("malformed manifest_hash")
    if (this.election_base_hash.import() == null) errs.add("malformed election_base_hash")

    return if (errs.hasErrors() || constants == null || manifestBytes == null) null
    else ElectionConfig(
                this.config_version,
                constants,
                this.number_of_guardians,
                this.quorum,
                this.parameter_base_hash.import()!!,
                this.manifest_hash.import()!!,
                this.election_base_hash.import()!!,
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

fun ElectionConstantsJson.import(errs: ErrorMessages) : ElectionConstants? {
    if (this.large_prime.fromHex() == null) errs.add("malformed large_prime")
    if (this.small_prime.fromHex() == null) errs.add("malformed small_prime")
    if (this.cofactor.fromHex() == null) errs.add("malformed cofactor")
    if (this.generator.fromHex() == null) errs.add("malformed generator")

    return if (errs.hasErrors()) null
    else ElectionConstants(
        this.name,
        this.large_prime.fromHex()!!,
        this.small_prime.fromHex()!!,
        this.cofactor.fromHex()!!,
        this.generator.fromHex()!!,
    )
}
