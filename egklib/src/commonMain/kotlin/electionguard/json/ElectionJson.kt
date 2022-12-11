package electionguard.json

import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionConstants
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Guardian
import electionguard.ballot.LagrangeCoordinate
import electionguard.core.Base16.fromHex
import electionguard.core.Base16.toHex
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.core.normalize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("ElectionConstants")
data class ConstantsJson(
    val name: String,
    val large_prime: String,
    val small_prime: String,
    val cofactor: String,
    val generator: String,
)

fun ElectionConstants.publish() = ConstantsJson(
    this.name,
    this.largePrime.toHex(), // .normalize(512).toHex(),
    this.smallPrime.toHex(), // .normalize(32).toHex(),
    this.cofactor.normalize(481).toHex(),
    this.generator.normalize(512).toHex(),
)

fun ConstantsJson.import() = ElectionConstants(
    this.name,
    this.large_prime.fromHex()?: throw RuntimeException(),
    this.small_prime.fromHex()?: throw RuntimeException(),
    this.cofactor.fromHex()?: throw RuntimeException(),
    this.generator.fromHex()?: throw RuntimeException(),
)

////////////////////////////////////////////////////////////

@Serializable
@SerialName("ElectionContext")
data class ContextJson(
    val number_of_guardians: Int,
    val quorum: Int,
    val elgamal_public_key: ElementModPJson,
    val commitment_hash: UInt256Json, // LOOK where is this used?
    val manifest_hash: UInt256Json,
    val crypto_base_hash: UInt256Json,
    val crypto_extended_base_hash: UInt256Json,
)

fun ElectionInitialized.publish() = ContextJson(
    this.config.numberOfGuardians,
    this.config.quorum,
    this.jointPublicKey.publish(),
    UInt256.ONE.publish(), // TODO WRONG
    this.manifestHash.publish(),
    this.cryptoBaseHash.publish(),
    this.cryptoExtendedBaseHash.publish(),
)

fun ContextJson.import(group: GroupContext, electionConfig: ElectionConfig, guardians: List<Guardian>) : ElectionInitialized {
    return ElectionInitialized(
        electionConfig.copy(numberOfGuardians = this.number_of_guardians, quorum = this.quorum),
        this.elgamal_public_key.import(group)?: throw RuntimeException(),
        this.manifest_hash.import()?: throw RuntimeException(),
        this.crypto_base_hash.import()?: throw RuntimeException(),
        this.crypto_extended_base_hash.import()?: throw RuntimeException(),
        guardians.sortedBy { it.guardianId },
    )
}

////////////////////////////////////////////////////////////

@Serializable
@SerialName("LagrangeCoordinate")
data class LagrangeCoordinateJson(
    val guardian_id: String,
    val x_coordinate: Int,
    val lagrange_coefficient: ElementModQJson,
)

fun LagrangeCoordinate.publish() = LagrangeCoordinateJson(
    this.guardianId,
    this.xCoordinate,
    this.lagrangeCoefficient.publish(),
)

fun LagrangeCoordinateJson.import(group: GroupContext) = LagrangeCoordinate(
    this.guardian_id,
    this.x_coordinate,
    this.lagrange_coefficient.import(group)?: throw RuntimeException(),
)

////////////////////////////////////////////////////////////

@Serializable
@SerialName("Coefficients")
data class CoefficientsJson(
    val coefficients: Map<String, LagrangeCoordinateJson>,
)

fun List<LagrangeCoordinate>.publish() = CoefficientsJson(
    this.associate{ it.guardianId to it.publish() }
)

fun CoefficientsJson.import(group: GroupContext): List<LagrangeCoordinate> {
    return this.coefficients.values.map { it.import(group) }
}
