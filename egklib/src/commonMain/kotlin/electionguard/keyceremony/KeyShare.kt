package electionguard.keyceremony

import electionguard.core.ElementModQ
import electionguard.core.HashedElGamalCiphertext

/** Pi_(ℓ), guardian ℓ's share of guardian i's secret. */
data class KeyShare(
    val ownerXcoord: Int, // guardian i (owns the polynomial Pi) xCoordinate
    val polynomialOwner: String, // guardian i (owns the polynomial Pi) id
    val secretShareFor: String, // guardian l with coordinate ℓ
    val yCoordinate: ElementModQ, // Pi_(ℓ), guardian ℓ's share of guardian i's secret
)

/** Encrypted version of Pi_(ℓ). */
data class EncryptedKeyShare(
    val ownerXcoord: Int, // guardian i (owns the polynomial Pi) xCoordinate
    val polynomialOwner: String, // guardian i (owns the polynomial Pi)
    val secretShareFor: String, // guardian l with coordinate ℓ
    val encryptedCoordinate: HashedElGamalCiphertext, // El(Pi_(ℓ)), spec 2.0, eq 18
)