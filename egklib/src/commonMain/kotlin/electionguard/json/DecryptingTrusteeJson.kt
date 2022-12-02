package electionguard.json

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.toResultOr
import com.github.michaelbull.result.unwrap
import electionguard.core.ElGamalKeypair
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElGamalSecretKey
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrustee
import electionguard.keyceremony.KeyCeremonyTrustee
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("DecryptingTrustee")
data class DecryptingTrusteeJson(
    val guardian_id: String,
    val sequence_order: Int,
    val public_key: ElementModPJson,
    val secret_key: ElementModQJson,
    val keyShares: List<EncryptedKeyShareJson>,
)

fun KeyCeremonyTrustee.publishDecryptingTrusteeJson() = DecryptingTrusteeJson(
    this.id,
    this.xCoordinate,
    this.electionPublicKey().publish(),
    this.electionPrivateKey().publish(),
    this.myShareOfOthers.values.map { it.publish() },
)

fun DecryptingTrusteeJson.import(group: GroupContext): Result<DecryptingTrustee, String> {
    val publicKey = this.public_key.import(group)
        .toResultOr { "DecryptingTrustee ${this.guardian_id} publicKey was malformed or missing" }
    val secretKey = this.secret_key.import(group)
        .toResultOr { "DecryptingTrustee ${this.guardian_id} secretKey was malformed or missing" }
    val errors = getAllErrors(publicKey, secretKey)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    val keySharesN = this.keyShares.map { it.import(group) }
    val allgood = keySharesN.map { it != null }.reduce { a, b -> a && b }
    return if (!allgood) {
        Err("DecryptingTrustee ${this.guardian_id} import keyShares failed")
    }
    else {
        val keyShares = keySharesN.map { it!! }
        Ok(DecryptingTrustee(
            this.guardian_id,
            this.sequence_order,
            ElGamalKeypair(
                ElGamalSecretKey(secretKey.unwrap()),
                ElGamalPublicKey(publicKey.unwrap()),
            ),
            keyShares.associateBy { it.missingGuardianId },
        ))
    }
}
