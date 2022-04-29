package electionguard.keyceremony

import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.SchnorrProof
import electionguard.core.hasValidSchnorrProof

data class PublicKeys(
    val guardianId: String,
    val guardianXCoordinate: UInt,
    val coefficientCommitments: List<ElementModP>,
    val coefficientProofs: List<SchnorrProof>,
) {
    fun publicKey(): ElGamalPublicKey {
        return ElGamalPublicKey(coefficientCommitments[0])
    }

    fun isValid(): Boolean {
        var idx = 0
        for (proof in this.coefficientProofs) {
            if (!ElGamalPublicKey(coefficientCommitments[idx]).hasValidSchnorrProof(proof)) {
                return false
            }
            idx++
        }
        return true
    }
}

/**
 * A point on a secret polynomial, and commitments to verify this point for a designated guardian.
 * @param generatingGuardianId The Id of the guardian that generated this
 * @param designatedGuardianId The Id of the guardian to receive this backup, who might be missing at decryption
 * @param designatedGuardianXCoordinate The x coordinate of the designated guardian
 * @param generatingGuardianValue The generatingGuardian's polynomial value at designatedGuardianXCoordinate, P𝑖_{l}
 */
data class SecretKeyShare(
    val generatingGuardianId: String,
    val designatedGuardianId: String,
    val designatedGuardianXCoordinate: UInt,
    val generatingGuardianValue: ElementModQ,
    val errors: String = "",
)