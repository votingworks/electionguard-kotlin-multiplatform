package electionguard.json

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.toResultOr
import com.github.michaelbull.result.unwrap
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.keyceremony.KeyCeremonyTrustee
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("DecryptingTrustee")
data class DecryptingTrusteeJson(
    val guardian_id: String,
    val sequence_order: Int,
    val public_key: ElementModPJson,
    val key_share: ElementModQJson,
)

fun KeyCeremonyTrustee.publishDecryptingTrusteeJson() = DecryptingTrusteeJson(
    this.id,
    this.xCoordinate,
    this.electionPublicKey().publish(),
    this.keyShare().publish(),
)

fun DecryptingTrusteeJson.import(group: GroupContext): Result<DecryptingTrusteeDoerre, String> {
    val publicKey = this.public_key.import(group)
        .toResultOr { "DecryptingTrustee ${this.guardian_id} publicKey was malformed or missing" }
    val keyShare = this.key_share.import(group)
        .toResultOr { "DecryptingTrustee ${this.guardian_id} secretKey was malformed or missing" }
    val errors = getAllErrors(publicKey, keyShare)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }
    return Ok(
        DecryptingTrusteeDoerre(
            this.guardian_id,
            this.sequence_order,
            publicKey.unwrap(),
            keyShare.unwrap(),
        )
    )
}
