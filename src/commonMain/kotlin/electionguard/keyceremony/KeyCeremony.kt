package electionguard.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Guardian
import electionguard.core.Base16.toHex
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.HashedElGamalCiphertext
import electionguard.core.SchnorrProof
import electionguard.core.UInt256
import electionguard.core.getSystemDate
import electionguard.core.hasValidSchnorrProof
import electionguard.core.hashElements

/** Exchange publicKeys and secretShares among the trustees */
fun keyCeremonyExchange(trustees: List<KeyCeremonyTrusteeIF>): Result<KeyCeremonyResults, String> {

    // exchange PublicKeys
    val publicKeys: MutableList<PublicKeys> = mutableListOf()
    val publicKeyResults: MutableList<Result<PublicKeys, String>> = mutableListOf()
    trustees.forEach { t1 ->
        val t1Result = t1.sendPublicKeys()
        if (t1Result is Err) {
            publicKeyResults.add(t1Result)
        } else {
            val t1Keys = t1Result.unwrap()
            publicKeys.add(t1Keys)
            trustees.filter { it.id() != t1.id() }.forEach { t2 ->
                publicKeyResults.add(t2.receivePublicKeys(t1Keys))
            }
        }
    }

    var errors = publicKeyResults.getAllErrors()
    if (errors.isNotEmpty()) {
        return Err("runKeyCeremony failed exchanging public keys: ${errors.joinToString("\n")}")
    }

    // exchange SecretKeyShares, and validate them
    val secretKeyResults: MutableList<Result<SecretKeyShare, String>> = mutableListOf()
    trustees.forEach { t1 ->
        trustees.filter { it.id() != t1.id() }.forEach { t2 ->
            val sendSecretKeyShareResult = t1.sendSecretKeyShare(t2.id())
            if (sendSecretKeyShareResult is Ok) {
                secretKeyResults.add(t2.receiveSecretKeyShare(sendSecretKeyShareResult.unwrap()))
            } else {
                secretKeyResults.add(sendSecretKeyShareResult)
            }
        }
    }

    // LOOK we are not doing the challenge/response scenario. Not clear under what circumstance that is needed.

    errors = secretKeyResults.getAllErrors()
    if (errors.isNotEmpty()) {
        return Err("runKeyCeremony failed exchanging secret keys: ${errors.joinToString("\n")}")
    }

    return Ok(KeyCeremonyResults(publicKeys))
}

data class PublicKeys(
    val guardianId: String,
    val guardianXCoordinate: Int,
    val coefficientCommitments: List<ElementModP>,
    val coefficientProofs: List<SchnorrProof>,
) {
    init {
        require(guardianId.isNotEmpty())
        require(guardianXCoordinate > 0)
        require(coefficientCommitments.isNotEmpty())
        require(coefficientProofs.size == coefficientCommitments.size)
    }

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
    val designatedGuardianXCoordinate: Int,
    val encryptedCoordinate: HashedElGamalCiphertext,
) {
    init {
        require(generatingGuardianId.isNotEmpty())
        require(designatedGuardianId.isNotEmpty())
        require(designatedGuardianXCoordinate > 0)
    }
}

data class KeyCeremonyResults(
    val publicKeys: List<PublicKeys>,
) {
    val publicKeysSorted = publicKeys.sortedBy { it.guardianXCoordinate }

    fun makeElectionInitialized(
        config: ElectionConfig,
        metadata: Map<String, String> = emptyMap(),
    ): ElectionInitialized {
        val jointPublicKey: ElementModP =
            publicKeysSorted.map { it.coefficientCommitments[0] }.reduce { a, b -> a * b }

        // cryptoBaseHash
        val primes = config.constants
        val cryptoBaseHash: UInt256 = hashElements(
            primes.largePrime.toHex(),
            primes.smallPrime.toHex(),
            primes.generator.toHex(),
            config.numberOfGuardians,
            config.quorum,
            config.manifest.cryptoHash,
        )

        // cryptoExtendedBaseHash
        val commitments: MutableList<ElementModP> = mutableListOf()
        publicKeysSorted.forEach { commitments.addAll(it.coefficientCommitments) }
        val commitmentsHash = hashElements(commitments)
        val cryptoExtendedBaseHash: UInt256 = hashElements(cryptoBaseHash, commitmentsHash)

        val guardians: List<Guardian> = publicKeysSorted.map { makeGuardian(it) }

        val metadataAll = mutableMapOf(
            Pair("CreatedBy", "keyCeremonyExchange"),
            Pair("CreatedOn", getSystemDate().toString()),
        )
        metadataAll.putAll(metadata)

        return ElectionInitialized(
            config,
            jointPublicKey,
            config.manifest.cryptoHash,
            cryptoBaseHash,
            cryptoExtendedBaseHash,
            guardians,
            metadataAll,
        )
    }
}

private fun makeGuardian(publicKeys: PublicKeys): Guardian {
    return Guardian(
        publicKeys.guardianId,
        publicKeys.guardianXCoordinate,
        publicKeys.coefficientCommitments,
        publicKeys.coefficientProofs,
    )
}