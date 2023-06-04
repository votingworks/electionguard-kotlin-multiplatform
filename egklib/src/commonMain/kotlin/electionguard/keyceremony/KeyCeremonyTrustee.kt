package electionguard.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.core.*

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
    // all the secrets are in here
    private val polynomial: ElectionPolynomial = group.generatePolynomial(id, xCoordinate, quorum)

    // Other guardians' public keys, keyed by other guardian id.
    internal val otherPublicKeys: MutableMap<String, PublicKeys> = mutableMapOf()

    // My share of other's key, keyed by other guardian id.
    private val myShareOfOthers: MutableMap<String, PrivateKeyShare> = mutableMapOf()

    // Other guardians share of my key, keyed by other guardian id. Only used in KeyCeremony
    private val othersShareOfMyKey: MutableMap<String, PrivateKeyShare> = mutableMapOf()

    init {
        require(id.isNotEmpty())
        require(xCoordinate > 0)
        require(quorum > 0)
    }

    override fun id(): String = id

    override fun xCoordinate(): Int = xCoordinate

    override fun electionPublicKey(): ElementModP = polynomial.coefficientCommitments[0]

    override fun coefficientCommitments(): List<ElementModP> = polynomial.coefficientCommitments

    override fun coefficientProofs(): List<SchnorrProof> = polynomial.coefficientProofs

    override fun publicKeys(): Result<PublicKeys, String> {
        return Ok(PublicKeys(
            id,
            xCoordinate,
            polynomial.coefficientProofs,
        ))
    }

    // P(ℓ) = (P1 (ℓ) + P2 (ℓ) + · · · + Pn (ℓ)) mod q. eq 3.
    override fun keyShare(): ElementModQ {
        var result: ElementModQ = polynomial.valueAt(group, xCoordinate)
        myShareOfOthers.values.forEach{ result += it.yCoordinate }
        return result
    }

    // debugging
    internal fun electionPrivateKey(): ElementModQ = polynomial.coefficients[0]

    // debug only
    internal fun valueAt(group: GroupContext, xcoord: Int) : ElementModQ {
        return polynomial.valueAt(group, xcoord)
    }

    /** Receive publicKeys from another guardian.  */
    override fun receivePublicKeys(publicKeys: PublicKeys): Result<Boolean, String> {
        val errors = mutableListOf<Result<Boolean, String>>()
        if (publicKeys.guardianId == this.id) {
            errors.add( Err("Cant send '${publicKeys.guardianId}' public keys to itself"))
        }
        if (publicKeys.guardianXCoordinate < 1) {
            errors.add( Err("${this.id} receivePublicKeys from '${publicKeys.guardianId}': guardianXCoordinate must be >= 1"))
        }
        if (publicKeys.coefficientProofs.size != quorum) {
            errors.add( Err("${this.id} receivePublicKeys from '${publicKeys.guardianId}': needs ($quorum) coefficientProofs"))
        }
        if (errors.isEmpty()) {
            val validProofs: Result<Boolean, String> = publicKeys.validate()
            if (validProofs is Err) {
                return validProofs
            }
            otherPublicKeys[publicKeys.guardianId] = publicKeys
        }
        return errors.merge()
    }

    /** Create another guardian's share of my key, encrypted. */
    override fun encryptedKeyShareFor(otherGuardian: String): Result<EncryptedKeyShare, String> {
        var pkeyShare = othersShareOfMyKey[otherGuardian]
        if (pkeyShare == null) {
            val other = otherPublicKeys[otherGuardian]
                ?: return Err("Trustee '$id', does not have public key for '$otherGuardian'")

            // Compute my polynomial's y value at the other's x coordinate = Pi(ℓ)
            val Pil: ElementModQ = polynomial.valueAt(group, other.guardianXCoordinate)
            // The encryption of that, using the other's public key. See spec 1.52, section 3.2.2 eq 14.
            val nonce: ElementModQ = other.publicKey().context.randomElementModQ(minimum = 2)
            val EPil = Pil.byteArray().hashedElGamalEncrypt(other.publicKey(), nonce)

            // keep track in case its challenged
            pkeyShare = PrivateKeyShare(
                this.id,
                other.guardianId,
                EPil,
                Pil,
            )
            othersShareOfMyKey[other.guardianId] = pkeyShare
        }

        return Ok(pkeyShare.makeEncryptedKeyShare())
    }

    /** Receive and verify a secret key share. */
    override fun receiveEncryptedKeyShare(share: EncryptedKeyShare?): Result<Boolean, String> {
        if (share == null) {
            return Err("ReceiveEncryptedKeyShare '${this.id}' sent a null share")
        }
         if (share.secretShareFor != id) {
            return Err("ReceiveEncryptedKeyShare '${this.id}' sent share to wrong trustee '${this.id}', should be availableGuardianId '${share.secretShareFor}'")
        }

        // decrypt Pi(l)
        val secretKey = ElGamalSecretKey(this.electionPrivateKey())
        val byteArray = share.encryptedCoordinate.decrypt(secretKey)
            ?: return Err("Trustee '$id' couldnt decrypt EncryptedKeyShare for missingGuardianId '${share.polynomialOwner}'")
        val expected: ElementModQ = byteArray.toUInt256().toElementModQ(group) // Pi(ℓ)

        // The other's Kij
        val publicKeys = otherPublicKeys[share.polynomialOwner]
            ?: return Err("Trustee '$id' does not have public keys for missingGuardianId '${share.polynomialOwner}'")

        // Is that value consistent with the missing guardian's commitments?
        // verify spec 1.52, sec 3.2.2 eq 16: g^Pi(ℓ) = Prod{ (Kij)^ℓ^j }, for j=0..k-1
        if (group.gPowP(expected) != calculateGexpPiAtL(this.xCoordinate, publicKeys.coefficientCommitments())) {
            return Err("Trustee '$id' failed to validate EncryptedKeyShare for missingGuardianId '${share.polynomialOwner}'")
        }

        myShareOfOthers[share.polynomialOwner] = PrivateKeyShare(
            share.polynomialOwner,
            this.id,
            share.encryptedCoordinate,
            expected,
        )
        return Ok(true)
    }

    /** Create another guardian's share of my key, not encrypted. */
    override fun keyShareFor(otherGuardian: String): Result<KeyShare, String> {
        val pkeyShare = othersShareOfMyKey[otherGuardian]
        return if (pkeyShare != null) Ok(pkeyShare.makeKeyShare())
            else Err("Trustee '$id', does not have KeyShare for '$otherGuardian'; must call encryptedKeyShareFor() first")
    }

    /** Receive and verify a key share. */
    override fun receiveKeyShare(keyShare: KeyShare): Result<Boolean, String> {
        val errors = mutableListOf<Result<Boolean, String>>()
        val myPublicKey = ElGamalPublicKey(this.electionPublicKey())

        if (keyShare.secretShareFor != id) {
            return Err("Sent KeyShare to wrong trustee '${this.id}', should be availableGuardianId '${keyShare.secretShareFor}'")
        }

        /* spec 1.52 says:
        If the recipient guardian Tℓ reports not receiving a suitable value Pi (ℓ), it becomes incumbent on the
        sending guardian Ti to publish this Pi (ℓ) together with the nonce Ri,ℓ it used to encrypt Pi (ℓ)
        under the public key Kℓ of recipient guardian Tℓ . If guardian Ti fails to produce a suitable Pi (ℓ)
        and nonce Ri,ℓ that match both the published encryption and the above equation, it should be
        excluded from the election and the key generation process should be restarted with an alternate
        guardian. If, however, the published Pi (ℓ) and Ri,ℓ satisfy both the published encryption and the
        equation above, the claim of malfeasance is dismissed, and the key generation process continues
        undeterred.19

        But in discussions with Josh 11/9/22, he says:

        As, I’m seeing things, the nonces aren’t relevant and never need to be supplied.  Guardian  is supposed to send
        Guardian  the share value  by encrypting it as  and handing it off.  If Guardian  claims to have not received a
        satisfactory , Guardian  is supposed to simply publish  so that anyone can check its validity directly.
        The verification is against the previously committed versions of coefficients  instead of against the
        transmitted encryption.  This prevents observers from needing to adjudicate whether or not Guardian
        sent a correct value initially.

        So im disabling the nonce exchange, and wait for spec 2 before continuing this.
         */

        /*
        val encryptedKeyShare = myShareOfOthers[keyShare.missingGuardianId] // what they sent us before
        if (encryptedKeyShare == null) {
            errors.add(Err("Trustee '$id', does not have encryptedKeyShare for missingGuardianId '${keyShare.missingGuardianId}';" +
                " must call receiveSecretKeyShare() first"))
        } else {
            // check that we can use the nonce to decrypt the El(Pi(ℓ)) that was sent previously
            val d = encryptedKeyShare.encryptedCoordinate.decryptWithNonce(myPublicKey, keyShare.nonce)
            if (d == null) {
                errors.add(Err("Trustee '$id' couldnt decrypt encryptedKeyShare for missingGuardianId '${keyShare.missingGuardianId}'"))
            } else {
                // Check if the decrypted value matches the Pi(ℓ) that was sent.
                val expected: ElementModQ = d.toUInt256().toElementModQ(group) // Pi(ℓ)
                if (expected != keyShare.coordinate) {
                    errors.add(Err("Trustee '$id' receiveKeyShare for '${keyShare.missingGuardianId}' decrypted KeyShare doesnt match"))
                }
            }
        }
         */

        val otherKeys = otherPublicKeys[keyShare.polynomialOwner]
        if (otherKeys == null) {
            errors.add(Err("Trustee '$id', does not have public key for missingGuardianId '${keyShare.polynomialOwner}'"))
        } else {
            // check if the Pi(ℓ) that was sent satisfies eq 16.
            // verify spec 1.52, sec 3.2.2 eq 16: g^Pi(ℓ) = Prod{ (Kij)^ℓ^j }, for j=0..k-1
            if (group.gPowP(keyShare.yCoordinate) != calculateGexpPiAtL(this.xCoordinate, otherKeys.coefficientCommitments())) {
                errors.add(Err("Trustee '$id' failed to validate KeyShare for missingGuardianId '${keyShare.polynomialOwner}'"))
            } else {
                // ok use it, but encrypt it ourself, dont use passed value, and use a new nonce
                val EPil = keyShare.yCoordinate.byteArray().hashedElGamalEncrypt(myPublicKey)
                myShareOfOthers[keyShare.polynomialOwner] = PrivateKeyShare(
                    keyShare.polynomialOwner,
                    keyShare.secretShareFor,
                    EPil,
                    keyShare.yCoordinate,
                )
            }
        }

        return errors.merge()
    }
}

// internal use only
private data class PrivateKeyShare(
    val polynomialOwner: String, // guardian j (owns the polynomial Pj)
    val secretShareFor: String, // guardian l with coordinate ℓ
    val encryptedCoordinate: HashedElGamalCiphertext, // El(Pj_(ℓ))
    val yCoordinate: ElementModQ, // Pj_(ℓ), trustee ℓ's share of trustee j's secret
) {
    init {
        require(polynomialOwner.isNotEmpty())
        require(secretShareFor.isNotEmpty())
    }

    fun makeEncryptedKeyShare() = EncryptedKeyShare(
        polynomialOwner,
        secretShareFor,
        encryptedCoordinate,
    )

    fun makeKeyShare() = KeyShare(
        polynomialOwner,
        secretShareFor,
        yCoordinate,
    )
}