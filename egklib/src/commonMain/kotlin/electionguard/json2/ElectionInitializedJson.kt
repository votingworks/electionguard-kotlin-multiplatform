package electionguard.json2

import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Guardian
import electionguard.core.*
import electionguard.util.ErrorMessages
import kotlinx.serialization.Serializable

@Serializable
data class ElectionInitializedJson(
    val joint_public_key: ElementModPJson, // aka K
    val extended_base_hash: UInt256Json, // aka He
    val guardians: List<GuardianJson>, // size = number_of_guardians
    val metadata: Map<String, String> = emptyMap(),
)
fun ElectionInitialized.publishJson() = ElectionInitializedJson(
    this.jointPublicKey.publishJson(),
    this.extendedBaseHash.publishJson(),
    this.guardians.map { it.publishJson() },
    )

fun ElectionInitializedJson.import(group: GroupContext, config: ElectionConfig, errs: ErrorMessages) : ElectionInitialized? {
    val joint_public_key = this.joint_public_key.import(group)?: errs.addNull("malformed joint_public_key") as ElementModP?
    val extended_base_hash = this.extended_base_hash.import()?: errs.addNull("malformed extended_base_hash") as UInt256?
    val guardians = this.guardians.map { it.import(group, errs.nested("Guardian ${it.guardian_id}")) }

    return if (joint_public_key == null || extended_base_hash == null || errs.hasErrors()) null
    else ElectionInitialized(
        config,
        joint_public_key,
        extended_base_hash,
        guardians.filterNotNull(),
    )
}

@Serializable
data class GuardianJson(
    val guardian_id: String,
    val x_coordinate: Int, // use sequential numbering starting at 1; == i of T_i, K_i
    val coefficient_proofs: List<SchnorrProofJson> // size = quorum
)

fun Guardian.publishJson() = GuardianJson(this.guardianId, this.xCoordinate, this.coefficientProofs.map { it.publishJson() })

fun GuardianJson.import(group: GroupContext, errs: ErrorMessages) : Guardian? {
    val coefficientProofs : List<SchnorrProof?> = this.coefficient_proofs.mapIndexed { idx, it ->
        it.import(group, errs.nested("SchnorrProof $idx"))
    }
    return if (errs.hasErrors()) null
    else Guardian(
            this.guardian_id,
            this.x_coordinate,
            coefficientProofs.filterNotNull(), // no errs means no nulls
        )
}

@Serializable
data class SchnorrProofJson(
    val public_key : ElementModPJson,
    val challenge : ElementModQJson,
    val response : ElementModQJson,
)

fun SchnorrProof.publishJson() = SchnorrProofJson(this.publicKey.publishJson(), this.challenge.publishJson(), this.response.publishJson())

fun SchnorrProofJson.import(group: GroupContext, errs: ErrorMessages = ErrorMessages("SchnorrProofJson.import")) : SchnorrProof?  {
    val publicKey = this.public_key.import(group) ?: (errs.addNull("malformed publicKey") as ElementModP?)
    val challenge = this.challenge.import(group) ?: (errs.addNull("malformed challenge") as ElementModQ?)
    val response = this.response.import(group) ?: (errs.addNull("malformed response") as ElementModQ?)

    return if (publicKey == null || challenge == null || response == null) null
        else SchnorrProof(publicKey, challenge, response)
}