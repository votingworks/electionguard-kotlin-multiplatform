package electionguard.core

/**
 * An "exponential ElGamal ciphertext" (i.e., with the plaintext in the exponent to allow for
 * homomorphic addition). (See [ElGamal 1982](https://ieeexplore.ieee.org/abstract/document/1057074))
 *
 * In a "normal" ElGamal encryption where the message goes into the exponent, a secret key `a`
 * with corresponding public key `g^a`, message `M` and nonce `R` would be encoded as the tuple
 * `(g^R, (g^a)^r * g^M)`.
 *
 * In this particular ElGamal implementation, we're instead encoding the ciphertext as
 * `(g^R, (g^a)^{R+M})`. This accelerates both the encryption process and the process of generating
 * the corresponding Chaum-Pedersen proofs.
 *
 * This also means that this ElGamal ciphertext is *not compatible with ElectionGuard 1.0*, but
 * is anticipated to be the standard for ElectionGuard 2.0 and later.
 */
data class ElGamalCiphertext(val pad: ElementModP, val data: ElementModP) {
    override fun toString() = "pad=${pad.toStringShort()} data=${data.toStringShort()} "


    /** Decrypts using the secret key from the keypair. If the decryption fails, `null` is returned. */
    fun decrypt(keypair: ElGamalKeypair): Int? {
        compatibleContextOrFail(pad, data, keypair.secretKey.key)
        val blind = pad powP keypair.secretKey.negativeKey
        val kPowM = data * blind

        return keypair.publicKey.dLog(kPowM)
    }

    /** Compute the share of the decryption from the secret key. */
    fun computeShare(secretKey: ElGamalSecretKey): ElementModP {
        compatibleContextOrFail(pad, data, secretKey.key)
        return pad powP secretKey.key
    }

    fun decryptWithShares(publicKey: ElGamalPublicKey, shares: Iterable<ElementModP>): Int? {
        val sharesList = shares.toList()
        val context = compatibleContextOrFail(pad, data, publicKey.key, *(sharesList.toTypedArray()))
        val allSharesProductM: ElementModP = with(context) { sharesList.multP() }
        val decryptedValue: ElementModP = this.data / allSharesProductM
        return publicKey.dLog(decryptedValue)
    }

    /** Decrypts a message by knowing the nonce. If the decryption fails, `null` is returned. */
    fun decryptWithNonce(publicKey: ElGamalPublicKey, nonce: ElementModQ, maxResult: Int = -1): Int? {
        compatibleContextOrFail(pad, data, publicKey.key, nonce)

        val blind = publicKey powP (-nonce)
        val kPowM = data * blind // data * blind = publicKey ^ m
        return publicKey.dLog(kPowM, maxResult)
    }

    /** Homomorphically "adds" two ElGamal ciphertexts together through piecewise multiplication. */
    operator fun plus(o: ElGamalCiphertext): ElGamalCiphertext {
        compatibleContextOrFail(this.pad, this.data, o.pad, o.data)
        return ElGamalCiphertext(pad * o.pad, data * o.data)
    }

    // reencrypt with another nonce. used in mixnet
    fun reencrypt(publicKey: ElGamalPublicKey, nonce: ElementModQ): ElGamalCiphertext {
        // Encr(m) = (g^ξ, K^(m+ξ)) = (a, b)
        // ReEncr(m)  = (g^(ξ+ξ'), K^(m+ξ+ξ')) = (a * g^ξ', b * K^ξ')
        // Encr(0) = (g^ξ', K^ξ') = (a', b'), so ReEncr(m) = (a * a', b * b') =  Encr(0) * Encr(m)

        val group = publicKey.context
        val ap = group.gPowP(nonce)
        val bp = publicKey.key powP nonce
        return ElGamalCiphertext(this.pad * ap, this.data * bp)
    }

}

/**
 * Homomorphically "adds" a sequence of ElGamal ciphertexts through piecewise multiplication.
 * @throws ArithmeticException if the sequence is empty
 */
fun List<ElGamalCiphertext>.encryptedSum(): ElGamalCiphertext? {
    return if (this.isEmpty()) null else this.reduce { a, b -> a + b }
}

/** Add two lists by component-wise multiplication */
fun List<ElGamalCiphertext>.add(other: List<ElGamalCiphertext>): List<ElGamalCiphertext> {
    require(this.size == other.size)
    val result = mutableListOf<ElGamalCiphertext>()
    this.forEachIndexed { index, value ->
        result.add(value + other[index])
    }
    return result
}

fun Int.encrypt(
    keypair: ElGamalKeypair,
    nonce: ElementModQ = keypair.context.randomElementModQ(minimum = 1)
) = this.encrypt(keypair.publicKey, nonce)

/** Encrypt an Int. */
fun Int.encrypt(
    publicKey: ElGamalPublicKey,
    nonce: ElementModQ = publicKey.context.randomElementModQ(minimum = 1)
): ElGamalCiphertext {
    val context = compatibleContextOrFail(publicKey.key, nonce)

    // LOOK: Exception
    if (nonce.isZero()) {
        throw ArithmeticException("Can't use a zero nonce for ElGamal encryption")
    }

    if (this < 0) {
        throw ArithmeticException("Can't encrypt a negative vote")
    }

    // We don't have to check if message >= Q, because it's an integer, and Q is much larger than that.
    // Enc(σ, ξ) = (α, β) = (g^ξ mod p, K^σ · K^ξ mod p) = (g^ξ mod p, K^(σ+ξ) mod p). spec 2.0.0 eq 24
    val pad = context.gPowP(nonce)
    val data = publicKey.key powP (nonce + this.toElementModQ(context))

    return ElGamalCiphertext(pad, data)
}

/** Encrypt a Long. Used to encrypt serial number. */
fun Long.encrypt(
    publicKey: ElGamalPublicKey,
    nonce: ElementModQ = publicKey.context.randomElementModQ(minimum = 1)
): ElGamalCiphertext {
    val context = compatibleContextOrFail(publicKey.key, nonce)

    // LOOK: Exception
    if (nonce.isZero()) {
        throw ArithmeticException("Can't use a zero nonce for ElGamal encryption")
    }

    if (this < 0) {
        throw ArithmeticException("Can't encrypt a negative value")
    }

    // We don't have to check if message >= Q, because it's a long, and Q is much larger than that.
    // Enc(σ, ξ) = (α, β) = (g^ξ mod p, K^σ · K^ξ mod p) = (g^ξ mod p, K^(σ+ξ) mod p). spec 2.0.0 eq 24
    val pad = context.gPowP(nonce)
    val data = publicKey.key powP (nonce + this.toElementModQ(context))

    return ElGamalCiphertext(pad, data)
}