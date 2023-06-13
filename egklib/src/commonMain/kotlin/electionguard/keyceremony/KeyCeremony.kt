package electionguard.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Guardian
import electionguard.core.*

private const val debug = false

/** Exchange PublicKeys and secret KeyShares among the trustees */
fun keyCeremonyExchange(trustees: List<KeyCeremonyTrusteeIF>, allowEncryptedFailure : Boolean = false): Result<KeyCeremonyResults, String> {
    // make sure trustee ids are all different
    val uniqueIds = trustees.map{it.id()}.toSet()
    if (uniqueIds.size != trustees.size) {
        return Err("keyCeremonyExchange trustees have non-unique ids = ${trustees.map{it.id()}}")
    }

    // make sure trustee xcoords are all different
    val uniqueCoords = trustees.map{it.xCoordinate()}.toSet()
    if (uniqueCoords.size != trustees.size) {
        return Err("keyCeremonyExchange trustees have non-unique xcoordinates = ${trustees.map{it.xCoordinate()}}")
    }

    // make sure trustee quorum are all the same
    val uniqueQuorum = trustees.map{it.coefficientCommitments().size}.toSet()
    if (uniqueQuorum.size != 1) {
        return Err("keyCeremonyExchange trustees have different quorums = ${trustees.map{it.coefficientCommitments().size}}")
    }

    // LOOK if the trustees are not trusted, we could do other verification tests here.
    //  are the public keys valid?
    //  are the encrypted shares valid?
    //  are the unencrypted shares valid?

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

    val errors = publicKeyResults.merge()
    if (errors is Err) {
        return Err("keyCeremonyExchange failed exchanging public keys:\n ${errors.error}")
    }

    // exchange SecretKeyShares, and validate them
    val keyShareFailures: MutableList<KeyShareFailed> = mutableListOf()
    val encryptedKeyResults: MutableList<Result<Boolean, String>> = mutableListOf()
    trustees.forEach { owner ->
        trustees.filter { it.id() != owner.id() }.forEach { shareFor ->
            if (debug) println(" ${owner.id()} encryptedKeyShareFor() ${shareFor.id()}")
            val encryptedKeyShareResult = owner.encryptedKeyShareFor(shareFor.id())
            if (encryptedKeyShareResult is Err) {
                encryptedKeyResults.add(Err(encryptedKeyShareResult.unwrapError()))
                keyShareFailures.add(KeyShareFailed(owner, shareFor))
            } else {
                val secretKeyShare = encryptedKeyShareResult.unwrap()
                if (debug) println(
                    "  ${shareFor.id()} encryptedKeyShareFor() for ${owner.id()} " +
                            "(polynomialOwner ${secretKeyShare.polynomialOwner} secretShareFor ${secretKeyShare.secretShareFor})"
                )
                val receiveEncryptedKeyShareResult = shareFor.receiveEncryptedKeyShare(secretKeyShare)
                if (receiveEncryptedKeyShareResult is Err) {
                    encryptedKeyResults.add(receiveEncryptedKeyShareResult)
                    keyShareFailures.add(KeyShareFailed(owner, shareFor))
                }
            }
        }
    }

    // spec 1.9, p 21
    // If the recipient guardian Gℓ reports not receiving a suitable value Pi (ℓ), it becomes incumbent on the
    // sending guardian Gi to publish this Pi (ℓ) together with the nonce ξi,ℓ it used to encrypt Pi (ℓ)
    // under the public key Kℓ of recipient guardian Gℓ . If guardian Gi fails to produce a suitable Pi (ℓ)
    // and nonce ξi,ℓ that match both the published encryption and the above equation, it should be
    // excluded from the election and the key generation process should be restarted with an alternate guardian.
    //   If, however, the published Pi (ℓ) and ξi,ℓ satisfy both the published encryption and the
    // equation (19) above, the claim of malfeasance is dismissed, and the key generation process continues undeterred.
    //   It is also permissible to dismiss any guardian that makes a false claim of malfeasance. However, this is not
    // required as the sensitive information that is released as a result of the claim could have been released by the
    // claimant in any case.
    // TODO KeyShare should include ξi,ℓ

    // Phase Two: if any secretKeyShares fail to validate, send and validate KeyShares
    val keyResults: MutableList<Result<Boolean, String>> = mutableListOf()
    keyShareFailures.forEach {
        if (debug) println(" ${it.polynomialOwner.id()} secretShareFor ${it.secretShareFor.id()}")
        val keyShareResult = it.polynomialOwner.keyShareFor(it.secretShareFor.id())
        if (keyShareResult is Ok) {
            val keyShare = keyShareResult.unwrap()
            if (debug) println(" ${it.secretShareFor.id()} keyShareFor() ${keyShare.polynomialOwner}")
            keyResults.add(it.secretShareFor.receiveKeyShare(keyShare))
        } else {
            keyResults.add(Err(keyShareResult.unwrapError()))
        }
    }

    if (allowEncryptedFailure) {
        val keyResultAll = keyResults.merge()
        return if (keyResultAll is Ok) {
            Ok(KeyCeremonyResults(publicKeys))
        } else {
            val all = (keyResults + encryptedKeyResults).merge()
            Err("keyCeremonyExchange failed exchanging shares: ${all.unwrapError()}")
        }
    } else {
        val all = (keyResults + encryptedKeyResults).merge()
        return if (all is Ok) {
            Ok(KeyCeremonyResults(publicKeys))
        } else {
            Err("keyCeremonyExchange failed exchanging shares:\n ${all.unwrapError()}")
        }
    }
}

private data class KeyShareFailed(
    val polynomialOwner: KeyCeremonyTrusteeIF, // guardian j (owns the polynomial Pj)
    val secretShareFor: KeyCeremonyTrusteeIF, // guardian l with coordinate ℓ
)

/** An internal result class used during the key ceremony. */
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

        // He = H(HB ; 12, K, K1,0 , K1,1 , . . . , K1,k−1 , K2,0 , . . . , Kn,k−2 , Kn,k−1 ) spec 1.9 p.22, eq 20.
        val commitments: MutableList<ElementModP> = mutableListOf()
        publicKeysSorted.forEach { commitments.addAll(it.coefficientCommitments()) }
        val extendedBaseHash = hashFunction(config.electionBaseHash.bytes, 0x12.toByte(), jointPublicKey, commitments)

        val guardians: List<Guardian> = publicKeysSorted.map { makeGuardian(it) }

        val metadataAll = mutableMapOf(
            Pair("CreatedBy", "keyCeremonyExchange"),
            Pair("CreatedOn", getSystemDate().toString()),
        )
        metadataAll.putAll(metadata)

        return ElectionInitialized(
            config,
            jointPublicKey,
            extendedBaseHash,
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