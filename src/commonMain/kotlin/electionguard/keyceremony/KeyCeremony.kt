package electionguard.keyceremony

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
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
        for ((idx, proof) in this.coefficientProofs.withIndex()) {
            if (!ElGamalPublicKey(coefficientCommitments[idx]).hasValidSchnorrProof(proof)) {
                return false
            }
        }
        return true
    }
}

/**
 * A point on a secret polynomial, and commitments to verify this point for a designated guardian.
 * @param generatingGuardianId The Id of the guardian that generated this, who might be missing at decryption
 * @param designatedGuardianId The Id of the guardian to receive this backup, matches the DecryptingTrustee.id
 * @param designatedGuardianXCoordinate The x coordinate of the designated guardian
 * @param generatingGuardianValue The generatingGuardian's polynomial value at designatedGuardianXCoordinate, P𝑖_{l}
 */
data class SecretKeyShare(
    val generatingGuardianId: String,
    val designatedGuardianId: String,
    val designatedGuardianXCoordinate: UInt,
    val generatingGuardianValue: ElementModQ,
)

/** Exchange publicKeys and secretShares among the trustees */
fun keyCeremonyExchange(trustees: List<KeyCeremonyTrustee>): String? {
    // exchange PublicKeys
    val publicKeyResults: MutableList<Result<PublicKeys, String>> = mutableListOf()
    trustees.forEach { t1 ->
        trustees.forEach { t2 ->
            publicKeyResults.add(t1.receivePublicKeys(t2.sharePublicKeys()))
        }
    }

    var errors = publicKeyResults.getAllErrors()
    if (errors.isNotEmpty()) {
        return "runKeyCeremony failed exchanging public keys: ${errors.joinToString("\n")}"
    }

    // exchange SecretKeyShares
    val secretKeyResults: MutableList<Result<SecretKeyShare, String>> = mutableListOf()
    trustees.forEach { t1 ->
        trustees.forEach { t2 ->
            t2.receiveSecretKeyShare(t1.sendSecretKeyShare(t2.id))
        }
    }

    errors = secretKeyResults.getAllErrors()
    if (errors.isNotEmpty()) {
        return "runKeyCeremony failed exchanging secret keys: ${errors.joinToString("\n")}"
    }

    return null
}