package electionguard.core

/** A public and private keypair, suitable for doing ElGamal cryptographic operations. */
data class ElGamalKeypair(val secretKey: ElGamalSecretKey, val publicKey: ElGamalPublicKey) {
    init {
        compatibleContextOrFail(secretKey.key, publicKey.key)
    }

    val context: GroupContext
        get() = this.publicKey.context
}

/**
 * A wrapper around an ElementModP that allows us to ensure that we're accelerating exponentiation
 * when using the key. Also contains the [inverseKey] (i.e., the multiplicative inverse mod `p`)
 * and supports computing discrete logs ([dLog]) with the key as the base.
 */
class ElGamalPublicKey(inputKey: ElementModP) {
    val key = inputKey.acceleratePow()
    val inverseKey = inputKey.multInv() // not accelerated because not used for pow
    private val dlogger = dLoggerOf(inputKey)

    val context: GroupContext
        get() = this.key.context

    /** Helper function. `key powP e` is shorthand for `key.key powP e`. */
    infix fun powP(exponent: ElementModQ): ElementModP = key powP exponent

    /**
     * Given an element x for which there exists an e, such that (key)^e = x, this will find e,
     * so long as e is less than [maxResult], which, if unspecified, defaults to a platform-specific
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


    override fun equals(other: Any?) =
        when (other) {
            is ElementModP -> key == other
            is ElGamalPublicKey -> key == other.key
            else -> false
        }

    override fun hashCode(): Int = key.hashCode()

    override fun toString(): String = key.toString()
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
    val context: GroupContext
        get() = this.key.context

    override fun equals(other: Any?) =
        when (other) {
            is ElementModQ -> key == other
            is ElGamalSecretKey -> key == other.key
            else -> false
        }
    override fun hashCode(): Int = key.hashCode()
    override fun toString(): String = key.toString()
}


/**
 * Given an ElGamal secret key, derives the corresponding secret/public key pair.
 *
 * @throws ArithmeticException if the secret key is less than two
 */
fun elGamalKeyPairFromSecret(secret: ElementModQ) =
    ElGamalKeypair(ElGamalSecretKey(secret), ElGamalPublicKey(secret.context.gPowP(secret))) // eq 10

/** Generates a random ElGamal keypair. */
fun elGamalKeyPairFromRandom(context: GroupContext) =
    elGamalKeyPairFromSecret(context.randomElementModQ(minimum = 2))