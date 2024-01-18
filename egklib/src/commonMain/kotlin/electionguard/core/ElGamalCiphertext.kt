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