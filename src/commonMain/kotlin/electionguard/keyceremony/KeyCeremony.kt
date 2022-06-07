package electionguard.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.HashedElGamalCiphertext
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

    fun isValid(): Result<Boolean, String> {
        val checkProofs: MutableList<Result<Boolean, String>> = mutableListOf()
        for ((idx, proof) in this.coefficientProofs.withIndex()) {
            if (!ElGamalPublicKey(coefficientCommitments[idx]).hasValidSchnorrProof(proof)) {
                checkProofs.add(Err("Guardian $guardianId has invalid proof for coefficient $idx"))
            } else {
                checkProofs.add(Ok(true))
            }
        }
        return if (checkProofs.getAllErrors().isNotEmpty())
            Err(checkProofs.getAllErrors().joinToString("\n"))
        else
            Ok(true)
    }
}

/**
 * A point on a secret polynomial, and commitments to verify this point for a designated guardian.
 * @param generatingGuardianId The Id of the guardian that generated this, who might be missing at decryption
 * @param designatedGuardianId The Id of the guardian to receive this backup, matches the DecryptingTrustee.id
 * @param designatedGuardianXCoordinate The x coordinate of the designated guardian
 * @param encryptedCoordinate Encryption of generatingGuardian's polynomial value at designatedGuardianXCoordinate, El(P𝑖_{l})
 */
data class SecretKeyShare(
    val generatingGuardianId: String,
    val designatedGuardianId: String,
    val designatedGuardianXCoordinate: UInt,
    val encryptedCoordinate: HashedElGamalCiphertext,
)

/** Exchange publicKeys and secretShares among the trustees */
fun keyCeremonyExchange(trustees: List<KeyCeremonyTrustee>): Result<Boolean, String> {
    // exchange PublicKeys
    val publicKeyResults: MutableList<Result<PublicKeys, String>> = mutableListOf()
    trustees.forEach { t1 ->
        trustees.forEach { t2 ->
            publicKeyResults.add(t1.receivePublicKeys(t2.sharePublicKeys()))
        }
    }

    var errors = publicKeyResults.getAllErrors()
    if (errors.isNotEmpty()) {
        return Err("runKeyCeremony failed exchanging public keys: ${errors.joinToString("\n")}")
    }

    // exchange SecretKeyShares
    val secretKeyResults: MutableList<Result<SecretKeyShare, String>> = mutableListOf()
    trustees.forEach { t1 ->
        trustees.forEach { t2 ->
            secretKeyResults.add(t2.receiveSecretKeyShare(t1.sendSecretKeyShare(t2.id)))
        }
    }

    errors = secretKeyResults.getAllErrors()
    if (errors.isNotEmpty()) {
        return Err("runKeyCeremony failed exchanging secret keys: ${errors.joinToString("\n")}")
    }

    return Ok(true)
}