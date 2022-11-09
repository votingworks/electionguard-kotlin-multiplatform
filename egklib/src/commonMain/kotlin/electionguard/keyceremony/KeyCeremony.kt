package electionguard.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Guardian
import electionguard.core.*
import electionguard.core.Base16.toHex

private const val debug = true

/** Exchange publicKeys and secretShares among the trustees */
fun keyCeremonyExchange(trustees: List<KeyCeremonyTrusteeIF>): Result<KeyCeremonyResults, String> {

    // exchange PublicKeys
    val publicKeys: MutableList<PublicKeys> = mutableListOf()
    val publicKeyResults: MutableList<Result<Boolean, String>> = mutableListOf()
    trustees.forEach { t1 ->
        if (debug) println(" ${t1.id()} publicKeys()")
        val t1Result = t1.publicKeys()
        if (t1Result is Err) {
            publicKeyResults.add(t1Result)
        } else {
            val t1Keys = t1Result.unwrap()
            publicKeys.add(t1Keys)
            trustees.filter { it.id() != t1.id() }.forEach { t2 ->
                if (debug) println("  ${t2.id()} receivePublicKeys() from ${t1Keys.guardianId}")
                publicKeyResults.add(t2.receivePublicKeys(t1Keys))
            }
        }
    }

    var errors = publicKeyResults.getAllErrors()
    if (errors.isNotEmpty()) {
        return Err("keyCeremonyExchange failed exchanging public keys: ${errors.joinToString("\n")}")
    }

    // exchange SecretKeyShares, and validate them
    val keyShareFailures: MutableList<KeyShareFailed> = mutableListOf()
    val secretKeyResults: MutableList<Result<Boolean, String>> = mutableListOf()
    trustees.forEach { missing ->
        trustees.filter { it.id() != missing.id() }.forEach { avail ->
            if (debug) println(" ${missing.id()} secretKeyShareFor() ${avail.id()}")
            val secretKeyShareResult = missing.secretKeyShareFor(avail.id())
            if (secretKeyShareResult is Ok) {
                val secretKeyShare = secretKeyShareResult.unwrap()
                if (debug) println("  ${avail.id()} receiveSecretKeyShare() for ${missing.id()} " +
                        "(missing ${secretKeyShare.missingGuardianId} avail ${secretKeyShare.availableGuardianId})")
                secretKeyResults.add(avail.receiveSecretKeyShare(secretKeyShare))
            } else {
                // secretKeyShare fails to validate
                secretKeyResults.add(Err(secretKeyShareResult.unwrapError()))
                keyShareFailures.add(KeyShareFailed(missing, avail))
            }
        }
    }

    // Phase Two if any secretKeyShares fail to validate, send KeyShares
    keyShareFailures.forEach { it ->
        val keyShareResult = it.missingGuardian.keyShareFor(it.availableGuardian.id())
        if (keyShareResult is Ok) {
            secretKeyResults.add(it.availableGuardian.receiveKeyShare(keyShareResult.unwrap()))
        } else {
            secretKeyResults.add(Err(keyShareResult.unwrapError()))
        }
    }

    errors = secretKeyResults.getAllErrors()
    if (errors.isNotEmpty()) {
        return Err("keyCeremonyExchange failed exchanging secret key shares: ${errors.joinToString("\n")}")
    }

    return Ok(KeyCeremonyResults(publicKeys))
}

private data class KeyShareFailed(
    val missingGuardian: KeyCeremonyTrusteeIF, // guardian j (owns the polynomial Pj)
    val availableGuardian: KeyCeremonyTrusteeIF, // guardian l with coordinate ℓ
)

data class PublicKeys(
    val guardianId: String,
    val guardianXCoordinate: Int,
    val coefficientProofs: List<SchnorrProof>,
) {
    init {
        require(guardianId.isNotEmpty())
        require(guardianXCoordinate > 0)
        require(coefficientProofs.isNotEmpty())
    }

    fun publicKey(): ElGamalPublicKey {
        return ElGamalPublicKey(coefficientProofs[0].publicKey)
    }

    fun coefficientCommitments(): List<ElementModP> {
        return coefficientProofs.map { it.publicKey }
    }

    fun validate(): Result<Boolean, String> {
        val checkProofs: MutableList<Result<Boolean, String>> = mutableListOf()
        for ((idx, proof) in this.coefficientProofs.withIndex()) {
            val result = proof.validate()
            if (result is Err) {
                checkProofs.add(Err("  Guardian $guardianId has invalid proof for coefficient $idx " +
                                result.unwrapError()
                    )
                )
            }
        }
        return checkProofs.merge()
    }
}

/**
 * A point on a secret polynomial, and commitments to verify this point for a designated guardian.
 * @param missingGuardianId The Id of the guardian that owns the polynomial (j)
 * @param availableGuardianId The Id of the guardian to receive this, matches the DecryptingTrustee.id (l)
 * @param encryptedCoordinate Encryption of missingGuardianId's polynomial value at availableGuardian's xcoordinate, El(Pj_(ℓ))
 */
data class EncryptedKeyShare(
    val missingGuardianId: String, // guardian j (owns the polynomial Pj)
    val availableGuardianId: String, // guardian l with coordinate ℓ
    val encryptedCoordinate: HashedElGamalCiphertext, // El(Pj_(ℓ))
)

data class KeyShare(
    val missingGuardianId: String, // guardian j (owns the polynomial Pj)
    val availableGuardianId: String, // guardian l with coordinate ℓ
    val coordinate: ElementModQ, // Pj_(ℓ)
    val nonce: ElementModQ, // nonce that was used to encrypt El(Pj_(ℓ))
)

data class KeyCeremonyResults(
    val publicKeys: List<PublicKeys>,
) {
    val publicKeysSorted = publicKeys.sortedBy { it.guardianXCoordinate }

    fun makeElectionInitialized(
        config: ElectionConfig,
        metadata: Map<String, String> = emptyMap(),
    ): ElectionInitialized {
        val jointPublicKey: ElementModP =
            publicKeysSorted.map { it.publicKey().key }.reduce { a, b -> a * b }

        // cryptoBaseHash = Q
        // spec 3.51, section 3.1.2. The contents of [the manifest] are hashed together with the
        // prime modulus (p), subgroup order (q), generator (g), number of guardians (n), decryption quorum
        // threshold value (k), date, and jurisdictional information to form a base hash code (Q) which will
        // be incorporated into every subsequent hash computation in the election.
        // TODO: add date, and jurisdictional information ??
        val primes = config.constants
        val cryptoBaseHash: UInt256 = hashElements(
            primes.largePrime.toHex(),
            primes.smallPrime.toHex(),
            primes.generator.toHex(),
            config.numberOfGuardians,
            config.quorum,
            config.manifest.cryptoHash,
        )

        // cryptoExtendedBaseHash = Qbar
        val commitments: MutableList<ElementModP> = mutableListOf()
        publicKeysSorted.forEach { commitments.addAll(it.coefficientCommitments()) }
        // spec 1.52, eq 17 and 3.B
        val qbar: UInt256 = hashElements(cryptoBaseHash, jointPublicKey, commitments)

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
            qbar,
            guardians,
            metadataAll,
        )
    }
}

private fun makeGuardian(publicKeys: PublicKeys): Guardian {
    return Guardian(
        publicKeys.guardianId,
        publicKeys.guardianXCoordinate,
        publicKeys.coefficientProofs,
    )
}