package electionguard.keyceremony

import electionguard.core.ElementModQ
import electionguard.core.HashedElGamalCiphertext

/** Pj_(ℓ), guardian ℓ's share of guardian j's secret. */
data class KeyShare(
    val polynomialOwner: String, // guardian j (owns the polynomial Pj)
    val secretShareFor: String, // guardian l with coordinate ℓ
    val yCoordinate: ElementModQ, // Pj_(ℓ), guardian ℓ's share of guardian j's secret
)

/** Encrypted version of Pj_(ℓ). */
data class EncryptedKeyShare(
    val polynomialOwner: String, // guardian j (owns the polynomial Pj)
    val secretShareFor: String, // guardian l with coordinate ℓ
    val encryptedCoordinate: HashedElGamalCiphertext, // El(Pj_(ℓ)), spec 1.52, eq 14
)