package electionguard.json2

import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.util.ErrorMessages
import kotlinx.serialization.Serializable

/** These must stay private, not in the election record. */
@Serializable
data class TrusteeJson(
    val id: String,
    val x_coordinate: Int,
    val polynomial_coefficients: List<ElementModQJson>,
    val key_share: ElementModQJson,
)

fun KeyCeremonyTrustee.publishJson(): TrusteeJson {
    return TrusteeJson(
        this.id,
        this.xCoordinate,
        this.polynomial.coefficients.map { it.publishJson() },
        this.computeSecretKeyShare().publishJson(),
    )
}

fun TrusteeJson.importDecryptingTrustee(group: GroupContext, errs : ErrorMessages): DecryptingTrusteeDoerre? {
    val privateKey = this.polynomial_coefficients[0].import(group) ?: errs.addNull("malformed privateKey") as ElementModQ?
    val keyShare = this.key_share.import(group) ?: errs.addNull("malformed keyShare") as ElementModQ?
    return if (errs.hasErrors()) null
    else DecryptingTrusteeDoerre(
        this.id,
        this.x_coordinate,
        group.gPowP(privateKey!!),
        keyShare!!,
        )
}