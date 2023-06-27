package electionguard.json2

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Guardian
import electionguard.core.GroupContext
import electionguard.core.SchnorrProof
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

fun ElectionInitializedJson.import(group: GroupContext, config: ElectionConfig) = ElectionInitialized(
    config,
    this.joint_public_key.import(group),
    this.extended_base_hash.import(),
    this.guardians.map { it.import(group) },
)

@Serializable
data class GuardianJson(
    val guardian_id: String,
    val x_coordinate: Int, // use sequential numbering starting at 1; == i of T_i, K_i
    val coefficient_proofs: List<SchnorrProofJson> // size = quorum
)
fun Guardian.publishJson() = GuardianJson(this.guardianId, this.xCoordinate, this.coefficientProofs.map { it.publishJson() })
fun GuardianJson.import(group: GroupContext) = Guardian(this.guardian_id, this.x_coordinate, this.coefficient_proofs.map { it.import(group) })

@Serializable
data class SchnorrProofJson(
    val public_key : ElementModPJson,
    val challenge : ElementModQJson,
    val response : ElementModQJson,
)
fun SchnorrProof.publishJson() = SchnorrProofJson(this.publicKey.publishJson(), this.challenge.publishJson(), this.response.publishJson())
fun SchnorrProofJson.import(group: GroupContext) = SchnorrProof(this.public_key.import(group), this.challenge.import(group), this.response.import(group))
fun SchnorrProofJson.importResult(group: GroupContext): Result<SchnorrProof, String> {
    val p = this.public_key.import(group)
    val c = this.challenge.import(group)
    val r = this.response.import(group)
    return if (p == null || c == null || r == null) Err("failed to import SchnorrProofJson") else Ok(SchnorrProof(p, c, r))
}