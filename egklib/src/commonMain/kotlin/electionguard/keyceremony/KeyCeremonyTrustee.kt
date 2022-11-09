package electionguard.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElGamalSecretKey
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.HashedElGamalCiphertext
import electionguard.core.SchnorrProof
import electionguard.core.context
import electionguard.core.decrypt
import electionguard.core.decryptWithNonce
import electionguard.core.hashedElGamalEncrypt
import electionguard.core.merge
import electionguard.core.randomElementModQ
import electionguard.core.toElementModQ
import electionguard.core.toUInt256

/**
 * A Trustee that knows its own secret key and polynomial.
 * KeyCeremonyTrustee must stay private. Guardian is its public info in the election record.
 */
class KeyCeremonyTrustee(
    val group: GroupContext,
    val id: String,
    val xCoordinate: Int,
    val quorum: Int,
) : KeyCeremonyTrusteeIF {
    // all the secrets are in this
    private val polynomial: ElectionPolynomial = group.generatePolynomial(id, quorum)

    // Other guardians' public keys, keyed by other guardian id.
    internal val otherPublicKeys: MutableMap<String, PublicKeys> = mutableMapOf()

    // My share of other's key, keyed by other guardian id.
    internal val myShareOfOthers: MutableMap<String, EncryptedKeyShare> = mutableMapOf()

    // Other guardians share of my key, keyed by other guardian id. Only used in KeyCeremony
    private val othersShareOfMyKey: MutableMap<String, PrivateKeyShare> = mutableMapOf()

    init {
        require(xCoordinate > 0)
        require(quorum > 0)
    }

    override fun id(): String = id

    override fun xCoordinate(): Int = xCoordinate

    override fun electionPublicKey(): ElementModP = polynomial.coefficientCommitments[0]

    override fun coefficientCommitments(): List<ElementModP> = polynomial.coefficientCommitments

    override fun coefficientProofs(): List<SchnorrProof> = polynomial.coefficientProofs

    internal fun electionPrivateKey(): ElementModQ = polynomial.coefficients[0]

    override fun publicKeys(): Result<PublicKeys, String> {
        return Ok(PublicKeys(
            id,
            xCoordinate,
            polynomial.coefficientProofs,
        ))
    }

    /** Receive publicKeys from another guardian.  */
    override fun receivePublicKeys(publicKeys: PublicKeys): Result<Boolean, String> {
        if (publicKeys.guardianId == this.id) {
            return Err("Cant send ${publicKeys.guardianId} public keys to itself")
        }
        if (publicKeys.guardianXCoordinate < 1) {
            return Err("${publicKeys.guardianId}: guardianXCoordinate must be >= 1")
        }
        if (publicKeys.coefficientProofs.size != quorum) {
            return Err("${publicKeys.guardianId}: must have quorum ($quorum) coefficientProofs")
        }
        val validProofs: Result<Boolean, String> = publicKeys.validate()
        if (validProofs is Err) {
            return validProofs
        }

        otherPublicKeys[publicKeys.guardianId] = publicKeys
        return Ok(true)
    }

    /** Create another guardian's share of my key. */
    override fun secretKeyShareFor(otherGuardian: String): Result<EncryptedKeyShare, String> {
        var keyShare = othersShareOfMyKey[otherGuardian]
        if (keyShare == null) {
            val other = otherPublicKeys[otherGuardian]
                ?: return Err("Trustee '$id', does not have public key for '$otherGuardian'")

            // Compute my polynomial's y value at the other's x coordinate = Pi(ℓ)
            val Pil: ElementModQ = polynomial.valueAt(group, other.guardianXCoordinate)
            // The encryption of that, using the other's public key. See spec 1.52, section 3.2.2 eq 14.
            val nonce: ElementModQ = other.publicKey().context.randomElementModQ(minimum = 2)
            val EPil = Pil.byteArray().hashedElGamalEncrypt(other.publicKey(), nonce)

            // keep track in case its challenged
            keyShare = PrivateKeyShare(
                this.id,
                other.guardianId,
                EPil,
                Pil,
                nonce,
            )
            othersShareOfMyKey[other.guardianId] = keyShare
        }

        return Ok(keyShare.makeSecretKeyShare())
    }

    /** Receive and verify a secret key share. */
    override fun receiveSecretKeyShare(share: EncryptedKeyShare): Result<Boolean, String> {
         if (share.availableGuardianId != id) {
            return Err("Sent SecretKeyShare to wrong trustee ${this.id}, should be availableGuardianId ${share.availableGuardianId}")
        }

        // decrypt Pi(l)
        val secretKey = ElGamalSecretKey(this.electionPrivateKey())
        val byteArray = share.encryptedCoordinate.decrypt(secretKey)
            ?: return Err("Trustee $id couldnt decrypt SecretKeyShare for missingGuardianId ${share.missingGuardianId}")
        val expected: ElementModQ = byteArray.toUInt256().toElementModQ(group) // Pi(ℓ)

        // The other's Kij
        val publicKeys = otherPublicKeys[share.missingGuardianId]
            ?: return Err("Trustee $id does not have public keys for missingGuardianId ${share.missingGuardianId}")

        // Is that value consistent with the missing guardian's commitments?
        // verify spec 1.52, sec 3.2.2 eq 16: g^Pi(ℓ) = Prod{ (Kij)^ℓ^j }, for j=0..k-1
        if (group.gPowP(expected) != calculateGexpPiAtL(this.xCoordinate, publicKeys.coefficientCommitments())) {
            return Err("Trustee $id failed to validate SecretKeyShare for missingGuardianId ${share.missingGuardianId}")
        }

        myShareOfOthers[share.missingGuardianId] = share
        return Ok(true)
    }

    /** Create another guardian's share of my key, not encrypted. */
    override fun keyShareFor(otherGuardian: String): Result<KeyShare, String> {
        val keyShare = othersShareOfMyKey[otherGuardian]
        return if (keyShare != null) Ok(keyShare.makeKeyShare()) else Err("Trustee '$id', does not have KeyShare for '$otherGuardian'")
    }

    /** Receive and verify a key share. */
    override fun receiveKeyShare(keyShare: KeyShare): Result<Boolean, String> {
        val errors = mutableListOf<Result<Boolean, String>>()
        val myPublicKey = ElGamalPublicKey(this.electionPublicKey())

        if (keyShare.availableGuardianId != id) {
            return Err("Sent KeyShare to wrong trustee ${this.id}, should be availableGuardianId ${keyShare.availableGuardianId}")
        }

        val encryptedKeyShare = myShareOfOthers[keyShare.missingGuardianId] // what they sent us before
        if (encryptedKeyShare == null) {
            errors.add(Err("Trustee '$id', does not have encryptedKeyShare for missingGuardianId '${keyShare.missingGuardianId}'"))
        } else {
            // check that we can use the nonce to decrypt the El(Pi(ℓ)) that was sent previously
            val d = encryptedKeyShare.encryptedCoordinate.decryptWithNonce(myPublicKey, keyShare.nonce)
            if (d == null) {
                errors.add(Err("Trustee $id couldnt decrypt encryptedKeyShare for missingGuardianId ${keyShare.missingGuardianId}"))
            } else {
                // Check if the decrypted value matches the Pi(ℓ) that was sent.
                val expected: ElementModQ = d.toUInt256().toElementModQ(group) // Pi(ℓ)
                if (expected != keyShare.coordinate) {
                    errors.add(Err("Trustee $id receiveKeyShare for ${keyShare.missingGuardianId} decrypted KeyShare doesnt match"))
                }
            }
        }

        val otherKeys = otherPublicKeys[keyShare.missingGuardianId]
        if (otherKeys == null) {
            errors.add(Err("Trustee '$id', does not have public key for missingGuardianId '${keyShare.missingGuardianId}'"))
        } else {
            // check if the Pi(ℓ) that was sent satisfies eq 16.
            // verify spec 1.52, sec 3.2.2 eq 16: g^Pi(ℓ) = Prod{ (Kij)^ℓ^j }, for j=0..k-1
            if (group.gPowP(keyShare.coordinate) != calculateGexpPiAtL(this.xCoordinate, otherKeys.coefficientCommitments())) {
                errors.add(Err("Trustee $id failed to validate KeyShare for missingGuardianId ${keyShare.missingGuardianId}"))
            } else {
                // ok use it, but encrypt it ourself, dont use passed value, and use a new nonce
                val EPil = keyShare.coordinate.byteArray().hashedElGamalEncrypt(myPublicKey)
                myShareOfOthers[keyShare.missingGuardianId] = EncryptedKeyShare(
                    keyShare.missingGuardianId,
                    keyShare.availableGuardianId,
                    EPil,
                )
            }
        }

        return errors.merge()
    }
}

private data class PrivateKeyShare(
    val missingGuardianId: String, // guardian j (owns the polynomial Pj)
    val availableGuardianId: String, // guardian l with coordinate ℓ
    val encryptedCoordinate: HashedElGamalCiphertext, // El(Pj_(ℓ))
    val coordinate: ElementModQ, // Pj_(ℓ)
    val nonce: ElementModQ, // nonce used to encrypt El(Pj_(ℓ))
) {
    init {
        require(missingGuardianId.isNotEmpty())
        require(availableGuardianId.isNotEmpty())
    }

    fun makeSecretKeyShare() = EncryptedKeyShare(
        missingGuardianId,
        availableGuardianId,
        encryptedCoordinate,
    )

    fun makeKeyShare() = KeyShare(
        missingGuardianId,
        availableGuardianId,
        coordinate,
        nonce,
    )
}