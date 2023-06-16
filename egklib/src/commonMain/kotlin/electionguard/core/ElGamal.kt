package electionguard.core

/**
 * A wrapper around an ElementModP that allows us to ensure that we're accelerating exponentiation
 * when using the key. Also contains the [inverseKey] (i.e., the multiplicative inverse mod `p`)
 * and supports computing discrete logs ([dLog]) with the key as the base.
 */
class ElGamalPublicKey(inputKey: ElementModP) {
    val key = inputKey.acceleratePow()
    val inverseKey = inputKey.multInv() // not accelerated because not used for pow
    private val dlogger = dLoggerOf(inputKey)

    override fun equals(other: Any?) =
        when (other) {
            is ElementModP -> key == other
            is ElGamalPublicKey -> key == other.key
            else -> false
        }

    override fun hashCode(): Int = key.hashCode()

    override fun toString(): String = key.toString()

    /** Helper function. `key powP e` is shorthand for `key.key powP e`. */
    infix fun powP(exponent: ElementModQ): ElementModP = key powP exponent

    /**
     * Given an element x for which there exists an e, such that (key)^e = x, this will find e,
     * so long as e is less than [maxResult], which if unspecified defaults to a platform-specific
     * value designed not to consume too much memory (perhaps 10 million). This will consume O(e)
     * time, the first time, after which the results are memoized for all values between 0 and e,
     * for better future performance.
     *
     * If the result is not found, `null` is returned.
     *
     * Note: when using this to decrypt an election tally, where each ballot can contribute
     * at most one to each counter, a suitable argument for [maxResult] would be the total
     * number of ballots. This will terminate the computation early, if there's something
     * erroneous with the input data, while still ensuring that any legitimate total can
     * and will be computed.
     */
    fun dLog(input: ElementModP, maxResult: Int = -1): Int? = dlogger.dLog(input, maxResult)
}

/**
 * A wrapper around an ElementModQ that allows us to hang onto a pre-computed [negativeKey]
 * (i.e., the additive inverse mod `q`). The secret key must be in [2, Q).
 */
class ElGamalSecretKey(val key: ElementModQ) {
    init {
        if (key < key.context.TWO_MOD_Q)
            throw ArithmeticException("secret key must be in [2, Q)")
    }

    val negativeKey: ElementModQ = -key

    override fun equals(other: Any?) =
        when (other) {
            is ElementModQ -> key == other
            is ElGamalSecretKey -> key == other.key
            else -> false
        }

    override fun hashCode(): Int = key.hashCode()

    override fun toString(): String = key.toString()

}

/** A public and private keypair, suitable for doing ElGamal cryptographic operations. */
data class ElGamalKeypair(val secretKey: ElGamalSecretKey, val publicKey: ElGamalPublicKey) {
    init {
        compatibleContextOrFail(secretKey.key, publicKey.key)
    }
}

val ElGamalSecretKey.context: GroupContext
    get() = this.key.context

val ElGamalPublicKey.context: GroupContext
    get() = this.key.context

val ElGamalKeypair.context: GroupContext
    get() = this.publicKey.context

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
}

/**
 * Given an ElGamal secret key, derives the corresponding secret/public key pair.
 *
 * @throws ArithmeticException if the secret key is less than two
 */
fun elGamalKeyPairFromSecret(secret: ElementModQ) =
    ElGamalKeypair(ElGamalSecretKey(secret), ElGamalPublicKey(secret.context.gPowP(secret)))

/** Generates a random ElGamal keypair. */
fun elGamalKeyPairFromRandom(context: GroupContext) =
    elGamalKeyPairFromSecret(context.randomElementModQ(minimum = 2))

/**
 * Uses an ElGamal public key to encrypt a message. An optional nonce can be specified to make this
 * deterministic, or it will be chosen at random.
 *
 * @throws ArithmeticException if the nonce is zero or if the message is negative
 */
fun Int.encrypt(
    publicKey: ElGamalPublicKey,
    nonce: ElementModQ = publicKey.context.randomElementModQ(minimum = 1)
): ElGamalCiphertext {
    val context = compatibleContextOrFail(publicKey.key, nonce)

    if (nonce.isZero()) {
        throw ArithmeticException("Can't use a zero nonce for ElGamal encryption")
    }

    if (this < 0) {
        throw ArithmeticException("Can't encrypt a negative message")
    }

    // We don't have to check if message >= Q, because it's an integer, and Q
    // is much larger than that.
    // Enc(σ, ξ) = (α, β) = (g^ξ mod p, K^σ · K^ξ mod p) = (g^ξ mod p, K^σ+ξ mod p). (21)
    val pad = context.gPowP(nonce)
    val data = publicKey.key powP (nonce + this.toElementModQ(context))

    return ElGamalCiphertext(pad, data)
}

/**
 * Uses an ElGamal public key to encrypt a message. An optional nonce can be specified to make this
 * deterministic, or it will be chosen at random.
 */
fun Int.encrypt(
    keypair: ElGamalKeypair,
    nonce: ElementModQ = keypair.context.randomElementModQ(minimum = 1)
) = this.encrypt(keypair.publicKey, nonce)

/** Decrypts using the secret key from the keypair. If the decryption fails, `null` is returned. */
fun ElGamalCiphertext.decrypt(keypair: ElGamalKeypair): Int? {
    compatibleContextOrFail(pad, data, keypair.secretKey.key)
    val blind = pad powP keypair.secretKey.negativeKey
    val kPowM = data * blind

    return keypair.publicKey.dLog(kPowM)
}

/** Compute the share of the decryption from the secret key. */
fun ElGamalCiphertext.computeShare(secretKey: ElGamalSecretKey): ElementModP {
    compatibleContextOrFail(pad, data, secretKey.key)
    return pad powP secretKey.key
}

/** Compute the share of the decryption from the secret key. */
fun ElGamalCiphertext.computeShare(secretKey: ElementModQ): ElementModP {
    compatibleContextOrFail(pad, data, secretKey)
    return pad powP secretKey
}

/** Compute the share of the decryption from the secret key. */
fun ElementModP.computeShare(secretKey: ElementModQ): ElementModP {
    compatibleContextOrFail(this, secretKey)
    return this powP secretKey
}

fun ElGamalCiphertext.decryptWithShares(publicKey: ElGamalPublicKey, shares: Iterable<ElementModP>): Int? {
    val sharesList = shares.toList()
    val context = compatibleContextOrFail(pad, data, publicKey.key, *(sharesList.toTypedArray()))
    val allSharesProductM: ElementModP = with (context) { sharesList.multP() }
    val decryptedValue: ElementModP = this.data / allSharesProductM
    return publicKey.dLog(decryptedValue)
}

/** Decrypts a message by knowing the nonce. If the decryption fails, `null` is returned. */
fun ElGamalCiphertext.decryptWithNonce(publicKey: ElGamalPublicKey, nonce: ElementModQ, maxResult: Int = -1): Int? {
    compatibleContextOrFail(pad, data, publicKey.key, nonce)

    val blind = publicKey powP (-nonce)
    val kPowM = data * blind // data * blind = publicKey ^ m
    return publicKey.dLog(kPowM, maxResult)
}

/** Homomorphically "adds" two ElGamal ciphertexts together through piecewise multiplication. */
operator fun ElGamalCiphertext.plus(o: ElGamalCiphertext): ElGamalCiphertext {
    compatibleContextOrFail(this.pad, this.data, o.pad, o.data)
    return ElGamalCiphertext(pad * o.pad, data * o.data)
}

/**
 * Homomorphically "adds" a sequence of ElGamal ciphertexts through piecewise multiplication.
 * @throws ArithmeticException if the sequence is empty
 */
fun Iterable<ElGamalCiphertext>.encryptedSum(): ElGamalCiphertext =
    // This operation isn't defined on an empty list -- we'd have to have some way of getting
    // an encryption of zero, but we don't have the public key handy -- so we'll just raise
    // an exception on that, and otherwise we're fine.
    asSequence()
        .let {
            it.ifEmpty {
                throw ArithmeticException("Cannot sum an empty list of ciphertexts")
            }
                .reduce { a, b -> a + b }
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