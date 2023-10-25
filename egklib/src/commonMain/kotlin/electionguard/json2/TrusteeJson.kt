package electionguard.json2

import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.keyceremony.KeyCeremonyTrustee
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

fun TrusteeJson.importDecryptingTrustee(group: GroupContext): DecryptingTrusteeDoerre {
    return DecryptingTrusteeDoerre(this.id,
        this.x_coordinate,
        group.gPowP(this.polynomial_coefficients[0].import(group)),
        this.key_share.import(group),
        )
}