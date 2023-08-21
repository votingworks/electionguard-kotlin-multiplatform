package electionguard.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.core.*
import kotlin.experimental.xor

/**
 * A Trustee that knows its own secret key and polynomial.
 * KeyCeremonyTrustee must stay private. Guardian is its public info in the election record.
 */
open class KeyCeremonyTrustee(
    val group: GroupContext,
    val id: String,
    val xCoordinate: Int,
    val quorum: Int,
    val polynomial : ElectionPolynomial = group.generatePolynomial(id, xCoordinate, quorum)
) : KeyCeremonyTrusteeIF {

    // Other guardians' public keys
    internal val otherPublicKeys: MutableMap<String, PublicKeys> = mutableMapOf()

    // My share of other's key.
    internal val myShareOfOthers: MutableMap<String, PrivateKeyShare> = mutableMapOf()

    // Other guardians share of my key.
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

    // The value P(i) == G_i’s share of the secret key s = (s1 + s2 + · · · + sn )
    // == (P1 (ℓ) + P2 (ℓ) + · · · + Pn (ℓ)) mod q. spec 2.0.0, eq 65.
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
            val other : PublicKeys = otherPublicKeys[otherGuardian] // G_l's public keys
                ?: return Err("Trustee '$id', does not have public key for '$otherGuardian'")

            // Compute my polynomial's y value at the other's x coordinate = Pi(ℓ)
            val Pil: ElementModQ = polynomial.valueAt(group, other.guardianXCoordinate)
            // My encryption of Pil, using the other's public key. spec 2.0.0, section 3.2.2, p 24, eq 18.
            val EPil : HashedElGamalCiphertext = shareEncryption(Pil, other)

            pkeyShare = PrivateKeyShare(this.xCoordinate, this.id, other.guardianId, EPil, Pil)
            // keep track in case its challenged
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
        val pilbytes = shareDecryption(share)
            ?: return Err("Trustee '$id' couldnt decrypt EncryptedKeyShare for missingGuardianId '${share.polynomialOwner}'")
        val expectedPil: ElementModQ = pilbytes.toUInt256().toElementModQ(group) // Pi(ℓ)

        // The other's Kij
        val publicKeys = otherPublicKeys[share.polynomialOwner]
            ?: return Err("Trustee '$id' does not have public keys for missingGuardianId '${share.polynomialOwner}'")

        // Having decrypted each Pi (ℓ), guardian Gℓ can now verify its validity against
        // the commitments Ki,0 , Ki,1 , . . . , Ki,k−1 made by Gi to its coefficients by confirming that
        // g^Pi(ℓ) = Prod{ (Kij)^ℓ^j }, for j=0..k-1 ; spec 2.0.0 eq 21
        if (group.gPowP(expectedPil) != calculateGexpPiAtL(this.xCoordinate, publicKeys.coefficientCommitments())) {
            return Err("Trustee '$id' failed to validate EncryptedKeyShare for missingGuardianId '${share.polynomialOwner}'")
        }

        // keep track of this result
        myShareOfOthers[share.polynomialOwner] = PrivateKeyShare(
            share.ownerXcoord,
            share.polynomialOwner,
            this.id,
            share.encryptedCoordinate,
            expectedPil,
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

        if (keyShare.secretShareFor != id) {
            return Err("Sent KeyShare to wrong trustee '${this.id}', should be availableGuardianId '${keyShare.secretShareFor}'")
        }

        val otherKeys = otherPublicKeys[keyShare.polynomialOwner]
        if (otherKeys == null) {
            errors.add(Err("Trustee '$id', does not have public key for missingGuardianId '${keyShare.polynomialOwner}'"))
        } else {
            // check if the Pi(ℓ) that was sent satisfies eq 21.
            // g^Pi(ℓ) = Prod{ (Kij)^ℓ^j }, for j=0..k-1
            if (group.gPowP(keyShare.yCoordinate) != calculateGexpPiAtL(this.xCoordinate, otherKeys.coefficientCommitments())) {
                errors.add(Err("Trustee '$id' failed to validate KeyShare for missingGuardianId '${keyShare.polynomialOwner}'"))
            } else {
                // ok use it, but encrypt it ourself, dont use passed value, and use a new nonce
                val Pil: ElementModQ = polynomial.valueAt(group, otherKeys.guardianXCoordinate)
                val EPil : HashedElGamalCiphertext = this.shareEncryption(Pil, otherKeys)

                myShareOfOthers[keyShare.polynomialOwner] = PrivateKeyShare(
                    keyShare.ownerXcoord,
                    keyShare.polynomialOwner,
                    keyShare.secretShareFor,
                    EPil,
                    keyShare.yCoordinate,
                )
            }
        }

        return errors.merge()
    }

    private val label = "share_enc_keys"
    private val context = "share_encrypt"

    // guardian Gi encryption Eℓ of Pi(ℓ) at another guardian's Gℓ coordinate ℓ
    // see section 3.2.2 "share encryption"
    open fun shareEncryption(
        Pil : ElementModQ,
        other: PublicKeys,
        nonce: ElementModQ = group.randomElementModQ(minimum = 2)
    ): HashedElGamalCiphertext {

        val K_l = other.publicKey() // other's publicKey
        val hp = K_l.context.constants.hp.bytes
        val i = xCoordinate
        val l = other.guardianXCoordinate

        // (αi,ℓ , βi,ℓ ) = (g^ξi,ℓ mod p, K^ξi,ℓ mod p), ξi,ℓ = nonce
        // (α_i,ℓ , β_i,ℓ ) = (g^nonce mod p, Kℓ^nonce mod p) ;  spec 2.0.0, eq 14
        // by encrypting a zero, we achieve exactly this
        val (alpha, beta) = 0.encrypt(K_l, nonce)
        // ki,ℓ = H(HP ; 0x11, i, ℓ, Kℓ , αi,ℓ , βi,ℓ ) ; eq 15 "secret key"
        val kil = hashFunction(hp, 0x11.toByte(), i, l, K_l.key, alpha, beta).bytes

        // footnote 27
        // This key derivation uses the KDF in counter mode from SP 800-108r1. The second input to HMAC contains
        // the counter in the first byte, the UTF-8 encoding of the string "share_enc_keys" as the Label (encoding is denoted
        // by b(. . . ), see Section 5.1.4), a separation 00 byte, the UTF-8 encoding of the string "share_encrypt" concatenated
        // with encodings of the numbers i and ℓ of the sending and receiving guardians as the Context, and the final two bytes
        // specifying the length of the output key material as 512 bits in total.

        // Label = b(“share enc keys”, 14)
        // Context = b(“share_encrypt”, 13) ∥ b(i, 4) ∥ b(ℓ, 4)
        // k0 = HMAC(ki,ℓ , 0x01 ∥ Label ∥ 0x00 ∥ Context ∥ 0x0200) ; spec 2.0.0,  eq 16
        val k0 = hmacFunction(kil, 0x01.toByte(), label, 0x00.toByte(), context, i, l, 512).bytes
        // k1 = HMAC(ki,ℓ , 0x02 ∥ Label ∥ 0x00 ∥ Context ∥ 0x0200) ; eq 17
        val k1 = hmacFunction(kil, 0x02.toByte(), label, 0x00.toByte(), context, i, l, 512).bytes

        // spec 2.0.0, eq 19
        // Ci,ℓ,0 = g^ξi,ℓ mod p = C0 = g^nonce == alpha
        val c0: ElementModP = alpha
        // C1 = b(Pi(ℓ),32) ⊕ k1, where the symbol ⊕ denotes bitwise XOR.
        val pilBytes = Pil.byteArray()
        val c1 = ByteArray(32) { pilBytes[it] xor k1[it] }
        // Ci,ℓ,2 = HMAC(k0 , b(Ci,ℓ,0 , 512) ∥ Ci,ℓ,1 )
        // C2 = HMAC(k0, b(C0,512) ∥ C1 )
        val c2 = hmacFunction(k0, c0, c1)

        return HashedElGamalCiphertext(c0, c1, c2, pilBytes.size)
    }

    // Share decryption.
    // After receiving the ciphertext (Ci,ℓ,0 , Ci,ℓ,1 , Ci,ℓ,2 ) from guardian Gi , guardian
    // Gℓ decrypts it by computing βi,ℓ = (Ci,ℓ,0)^sℓ mod p, setting αi,ℓ = Ci,ℓ,0 and obtaining
    //   ki,ℓ = H(HP ; 11, i, ℓ, Kℓ , αi,ℓ , βi,ℓ ).
    // Now the MAC key k0 and the encryption key k1 can be computed as
    // above in Equations (16) and (17), which allows Gℓ to verify the validity of the MAC, namely that
    //   Ci,ℓ,2 = HMAC(k0 , b(Ci,ℓ,0 , 512) ∥ Ci,ℓ,1 ).
    // If the MAC verifies, then Gℓ decrypts b(Pi (ℓ), 32) = Ci,ℓ,1 ⊕ k1 .
    fun shareDecryption(share: EncryptedKeyShare): ByteArray?  {
        // αi,ℓ = Ci,ℓ,0
        // βi,ℓ = (Ci,ℓ,0 )sℓ mod p
        // ki,ℓ = H(HP ; 11, i, ℓ, Kℓ , αi,ℓ , βi,ℓ )
        val c0 = share.encryptedCoordinate.c0
        val c1 = share.encryptedCoordinate.c1

        val alpha = c0
        val beta = c0 powP this.electionPrivateKey()
        val hp = group.constants.hp.bytes
        val kil = hashFunction(hp, 0x11.toByte(), share.ownerXcoord, xCoordinate, electionPublicKey(), alpha, beta).bytes

        // Now the MAC key k0 and the encryption key k1 can be computed as above in Equations (16) and (17)
        val k0 = hmacFunction(kil, 0x01.toByte(), label, 0x00.toByte(), context, share.ownerXcoord, xCoordinate, 512).bytes
        val k1 = hmacFunction(kil, 0x02.toByte(), label, 0x00.toByte(), context, share.ownerXcoord, xCoordinate, 512).bytes

        // Gℓ can verify the validity of the MAC, namely that Ci,ℓ,2 = HMAC(k0 , b(Ci,ℓ,0 , 512) ∥ Ci,ℓ,1 ).
        val expectedC2 = hmacFunction(k0, c0, c1)
        if (expectedC2 != share.encryptedCoordinate.c2) {
            return null
        }

        //  If the MAC verifies, Gℓ decrypts b(Pi(ℓ), 32) = Ci,ℓ,1 ⊕ k1 .
        val pilBytes = ByteArray(32) { c1[it] xor k1[it] }
        return pilBytes
    }

}

// internal use only
internal data class PrivateKeyShare(
    val ownerXcoord: Int, // guardian i (owns the polynomial Pi) xCoordinate
    val polynomialOwner: String, // guardian i (owns the polynomial Pi)
    val secretShareFor: String, // guardian l with coordinate ℓ
    val encryptedCoordinate: HashedElGamalCiphertext, // El(Pi_(ℓ))
    val yCoordinate: ElementModQ, // Pi_(ℓ), trustee ℓ's share of trustee i's secret
) {
    init {
        require(polynomialOwner.isNotEmpty())
        require(secretShareFor.isNotEmpty())
    }

    fun makeEncryptedKeyShare() = EncryptedKeyShare(
        ownerXcoord,
        polynomialOwner,
        secretShareFor,
        encryptedCoordinate,
    )

    fun makeKeyShare() = KeyShare(
        ownerXcoord,
        polynomialOwner,
        secretShareFor,
        yCoordinate,
    )
}