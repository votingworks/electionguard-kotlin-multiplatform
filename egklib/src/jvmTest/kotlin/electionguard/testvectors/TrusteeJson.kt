package electionguard.testvectors

import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.keyceremony.regeneratePolynomial
import kotlinx.serialization.Serializable

@Serializable
data class TrusteeJson(
    val id: String,
    val xCoordinate: Int,
    val polynomial_coefficients: List<ElementModQJson>,
    val keyShare: ElementModQJson,
    val missing: Boolean,
)

fun KeyCeremonyTrustee.publishJson(missing : Boolean): TrusteeJson {
    return TrusteeJson(
        this.id,
        this.xCoordinate,
        this.polynomial.coefficients.map { it.publishJson() },
        this.keyShare().publishJson(),
        missing,
    )
}

fun TrusteeJson.importKeyCeremonyTrustee(group: GroupContext): KeyCeremonyTrustee {
    return KeyCeremonyTrustee(
        group,
        this.id,
        this.xCoordinate,
        polynomial_coefficients.size,
        group.regeneratePolynomial(
            this.id,
            this.xCoordinate,
            this.polynomial_coefficients.map { it.import(group) },
        )
    )
}

fun TrusteeJson.importDecryptingTrustee(group: GroupContext): DecryptingTrusteeDoerre {
    return DecryptingTrusteeDoerre(this.id,
        this.xCoordinate,
        group.gPowP(this.polynomial_coefficients[0].import(group)),
        this.keyShare.import(group))
}