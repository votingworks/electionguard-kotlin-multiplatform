package electionguard.core

import electionguard.ballot.ElectionConstants
import electionguard.core.Base16.fromHex
import electionguard.core.Base16.toHex
import electionguard.core.Base64.fromBase64
import electionguard.core.Base64.toBase64
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger("GroupCommon")

internal const val b64Production4096P256MinusQ = "AL0="
internal const val intProduction4096PBits = 4096

internal const val b64Production3072P256MinusQ = "AL0="
internal const val intProduction3072PBits = 3072

// 32-bit "tiny" group", suitable for accelerated testing
internal const val intTestP = 1879047647
internal const val intTestQ = 134217689
internal const val intTestR = 14
internal const val intTestG = 16384

internal val intTestMontgomeryI = 2147483648U
internal val intTestMontgomeryIMinus1 = 2147483647U
internal val intTestMontgomeryIPrime = 1533837288U
internal val intTestMontgomeryPPrime = 1752957409U

// 64-bit "small" group, suitable for accelerated testing
internal const val b64TestP = "b//93w=="
internal const val b64TestQ = "B///2Q=="
internal const val b64TestP256MinusQ = "AP/////////////////////////////////////4AAAn"
internal const val b64TestR = "Dg=="
internal const val b64TestG = "QAA="

internal const val b64TestMontgomeryI = "AIAAAAA="
internal const val b64TestMontgomeryIMinus1 = "f////w=="
internal const val b64TestMontgomeryIPrime = "W2x/6A=="
internal const val b64TestMontgomeryPPrime = "aHwB4Q=="

internal const val intTestPBits = 31

/**
 * ElectionGuard defines two modes of operation, with P having either 4096 bits or 3072 bits. We'll
 * track which mode we're using with this enum. For testing purposes, and only available to the
 * `test` modules, see `TinyGroup`, which provides "equivalent" modular arithmetic for
 * stress-testing code, while running significantly faster.
 */
enum class ProductionMode(val numBitsInP: Int) {
    Mode4096(intProduction4096PBits),
    Mode3072(intProduction3072PBits);

    override fun toString() = "ProductionMode($numBitsInP bits)"

    val numBytesInP: Int = numBitsInP / 8
    val numLongWordsInP: Int = numBitsInP / 64
}

/**
 * The GroupContext interface provides all the necessary context to define the arithmetic that we'll
 * be doing, such as the moduli P and Q, the generator G, and so forth. This also allows us to
 * encapsulate acceleration data structures that we'll use to support various operations.
 */
interface GroupContext {
    /**
     * Returns whether we're using "production primes" (bigger, slower, secure) versus "test primes"
     * (smaller, faster, but insecure).
     */
    fun isProductionStrength(): Boolean

    /**
     * A "description" of this group, suitable for serialization. Write this out alongside your
     * ballots or other external data, to represent the mathematical group used for all of its
     * cryptographic operations. When reading in external and possibly untrusted data, you can
     * deserialize the JSON back to this type, and then use the [isCompatible]
     * method to validate that external data is compatible with internal values.
     */
    val constants: ElectionConstants

    /** Useful constant: zero mod p */
    val ZERO_MOD_P: ElementModP

    /** Useful constant: one mod p */
    val ONE_MOD_P: ElementModP

    /** Useful constant: two mod p */
    val TWO_MOD_P: ElementModP

    /** Useful constant: the group generator */
    val G_MOD_P: ElementModP

    /** Useful constant: the inverse of the group generator */
    val GINV_MOD_P: ElementModP

    /** Useful constant: the group generator, squared */
    val G_SQUARED_MOD_P: ElementModP

    /** Useful constant: the modulus of the ElementModQ group */
    val Q_MOD_P: ElementModP

    /** Useful constant: zero mod q */
    val ZERO_MOD_Q: ElementModQ

    /** Useful constant: one mod q */
    val ONE_MOD_Q: ElementModQ

    /** Useful constant: two mod q */
    val TWO_MOD_Q: ElementModQ

    /**
     * Useful constant: the maximum number of bytes to represent any element mod p when serialized
     * as a `ByteArray`.
     */
    val MAX_BYTES_P: Int

    /**
     * Useful constant: the maximum number of bytes to represent any element mod q when serialized
     * as a `ByteArray`.
     */
    val MAX_BYTES_Q: Int

    /**
     * Useful constant: the number of bits it takes to represent any element mod p.
     */
    val NUM_P_BITS: Int

    /**
     * Identifies whether two internal GroupContexts are "compatible", so elements made in one
     * context would work in the other. Groups with the same primes will be compatible. Note that
     * this is meant to be fast, so only makes superficial checks. The [ElectionConstants] variant
     * of this method validates that all the group constants are the same.
     */
    fun isCompatible(ctx: GroupContext): Boolean

    /**
     * Identifies whether an external [ElectionConstants] is "compatible" with this GroupContext.
     */
    fun isCompatible(other: ElectionConstants): Boolean {
        return other.largePrime.contentEquals(constants.largePrime) && other.smallPrime.contentEquals(constants.smallPrime) &&
                other.generator.contentEquals(constants.generator) && other.cofactor.contentEquals(constants.cofactor)
    }

    /**
     * Converts a [ByteArray] to an [ElementModP]. The input array is assumed to be in big-endian
     * byte-order: the most significant byte is in the zeroth element; this is the same behavior as
     * Java's BigInteger. Guarantees the result is in [minimum, P), by computing the result mod P.
     */
    fun binaryToElementModPsafe(b: ByteArray, minimum: Int = 0): ElementModP

    /**
     * Converts a [ByteArray] to an [ElementModQ]. The input array is assumed to be in big-endian
     * byte-order: the most significant byte is in the zeroth element; this is the same behavior as
     * Java's BigInteger. Guarantees the result is in [minimum, Q), by computing the result mod Q.
     */
    fun binaryToElementModQsafe(b: ByteArray, minimum: Int = 0): ElementModQ

    /**
     * Converts a [ByteArray] to an [ElementModP]. The input array is assumed to be in big-endian
     * byte-order: the most significant byte is in the zeroth element; this is the same behavior as
     * Java's BigInteger. Returns null if the number is out of bounds or malformed.
     */
    fun binaryToElementModP(b: ByteArray): ElementModP?

    /**
     * Converts a [ByteArray] to an [ElementModQ]. The input array is assumed to be in big-endian
     * byte-order: the most significant byte is in the zeroth element; this is the same behavior as
     * Java's BigInteger. Returns null if the number is out of bounds.
     */
    fun binaryToElementModQ(b: ByteArray): ElementModQ?

    /**
     * Converts an integer to an ElementModQ, with optimizations when possible for small integers
     */
    fun uIntToElementModQ(i: UInt): ElementModQ

    /**
     * Converts an integer to an ElementModP, with optimizations when possible for small integers
     */
    fun uIntToElementModP(i: UInt): ElementModP

    /**
     * Computes the sum of the given elements, mod q; this can be faster than using the addition
     * operation for large numbers of inputs by potentially reusing scratch-space memory.
     */
    fun Iterable<ElementModQ>.addQ(): ElementModQ

    /**
     * Computes the product of the given elements, mod p; this can be faster than using the
     * multiplication operation for large numbers of inputs by potentially reusing scratch-space
     * memory.
     */
    fun Iterable<ElementModP>.multP(): ElementModP

    /** Computes G^e mod p, where G is our generator */
    fun gPowP(e: ElementModQ): ElementModP

    /**
     * Given an element x for which there exists an e, such that g^e = x, this will find e,
     * so long as e is less than [maxResult], which if unspecified defaults to a platform-specific
     * value designed not to consume too much memory (perhaps 10 million). This will consume O(e)
     * time, the first time, after which the results are memoized for all values between 0 and e,
     * for better future performance.
     */
    fun dLogG(p: ElementModP, maxResult: Int = - 1): Int?

    fun showAndClearCountPowP() : String
}

interface Element {
    /**
     * Every Element knows the [GroupContext] that was used to create it. This simplifies code that
     * computes with elements, allowing arithmetic expressions to be written in many cases without
     * needing to pass in the context.
     */
    val context: GroupContext

    /**
     * Normal computations should ensure that every [Element] is in the modular bounds defined by
     * the group, but deserialization of hostile inputs or buggy code might not preserve this
     * property, so it's valuable to have a way to check. This method allows anything in [0, N)
     * where N is the group modulus.
     */
    fun inBounds(): Boolean

    /**
     * Normal computations should ensure that every [Element] is in the modular bounds defined by
     * the group, but deserialization of hostile inputs or buggy code might not preserve this
     * property, so it's valuable to have a way to check. This method allows anything in [1, N)
     * where N is the group modulus.
     */
    fun inBoundsNoZero(): Boolean

    /** Checks whether this element is zero. */
    fun isZero(): Boolean

    /** Converts from any [Element] to a big-endian [ByteArray] representation. */
    fun byteArray(): ByteArray

    fun toHex() : String = byteArray().toHex()
}

interface ElementModQ : Element, Comparable<ElementModQ> {
    /** Modular addition */
    operator fun plus(other: ElementModQ): ElementModQ

    /** Modular subtraction */
    operator fun minus(other: ElementModQ): ElementModQ

    /** Modular multiplication */
    operator fun times(other: ElementModQ): ElementModQ

    /** Computes the additive inverse */
    operator fun unaryMinus(): ElementModQ

    /** Computes b^e mod q. LOOK seems to be not ever used */
    infix fun powQ(e: ElementModQ): ElementModQ

    /** Finds the multiplicative inverse */
    fun multInv(): ElementModQ

    /** Multiplies by the modular inverse of [denominator] */
    infix operator fun div(denominator: ElementModQ): ElementModQ

    /** Allows elements to be compared (<, >, <=, etc.) using the usual arithmetic operators. */
    override operator fun compareTo(other: ElementModQ): Int
}

interface ElementModP : Element, Comparable<ElementModP> {
    /**
     * Validates that this element is a quadratic residue, ie in Z_p^r.
     * "Z_p^r is the set of r-th-residues in Z∗p", see spec 2.0 p.9
     */
    fun isValidResidue(): Boolean

    /** Computes b^e mod p */
    infix fun powP(e: ElementModQ): ElementModP

    /** Modular multiplication */
    operator fun times(other: ElementModP): ElementModP

    /** Finds the multiplicative inverse */
    fun multInv(): ElementModP

    /** Multiplies by the modular inverse of [denominator] */
    infix operator fun div(denominator: ElementModP): ElementModP

    /** Allows elements to be compared (<, >, <=, etc.) using the usual arithmetic operators. */
    override operator fun compareTo(other: ElementModP): Int

    /**
     * Creates a new instance of this element where the `powP` function will use the acceleration
     * possible with `PowRadix` to run faster. The `PowRadixOption` for this instance is taken from
     * the `GroupContext`.
     */
    fun acceleratePow(): ElementModP

    /** Converts to Montgomery form, allowing for faster modular multiplication. */
    fun toMontgomeryElementModP(): MontgomeryElementModP

    /** Short version of the String for readability */
    fun toStringShort(): String {
        val s = base16()
        val len = s.length
        return "${s.substring(0, 7)}...${s.substring(len-8, len)}"
    }
}

/**
 * Computes the sum of the given elements, mod q; this can be faster than using the addition
 * operation for large numbers of inputs by potentially reusing scratch-space memory.
 */
fun GroupContext.addQ(vararg elements: ElementModQ) = elements.asIterable().addQ()

/**
 * Computes the product of the given elements, mod p; this can be faster than using the
 * multiplication operation for large numbers of inputs by potentially reusing scratch-space memory.
 */
fun GroupContext.multP(vararg elements: ElementModP) = elements.asIterable().multP()

/**
 * Converts a base-16 (hexadecimal) string to an [ElementModP]. Returns null if the number is out of
 * bounds or the string is malformed.
 */
fun GroupContext.base16ToElementModP(s: String): ElementModP? =
    s.fromHex()?.let { binaryToElementModP(it) }

/**
 * Converts a base-16 (hexadecimal) string to an [ElementModQ]. Returns null if the number is out of
 * bounds or the string is malformed.
 */
fun GroupContext.base16ToElementModQ(s: String): ElementModQ? =
    s.fromHex()?.let { binaryToElementModQ(it) }

/**
 * Converts a base-16 (hexadecimal) string to an [ElementModP]. Guarantees the result is in [0, P),
 * by computing the result mod P.
 */
fun GroupContext.base16ToElementModPsafe(s: String): ElementModP =
    s.fromHex()?.let { binaryToElementModPsafe(it) } ?: ZERO_MOD_P

/**
 * Converts a base-16 (hexadecimal) string to an [ElementModQ]. Guarantees the result is in [0, Q),
 * by computing the result mod Q.
 */
fun GroupContext.base16ToElementModQsafe(s: String): ElementModQ =
    s.fromHex()?.let { binaryToElementModQsafe(it) } ?: ZERO_MOD_Q

/**
 * Converts a base-64 string to an [ElementModP]. Returns null if the number is out of bounds or the
 * string is malformed.
 */
fun GroupContext.base64ToElementModP(s: String): ElementModP? =
    s.fromBase64()?.let { binaryToElementModP(it) }

/**
 * Converts a base-64 string to an [ElementModQ]. Returns null if the number is out of bounds or the
 * string is malformed.
 */
fun GroupContext.base64ToElementModQ(s: String): ElementModQ? =
    s.fromBase64()?.let { binaryToElementModQ(it) }

/**
 * Converts a base-64 string to an [ElementModP]. Guarantees the result is in [0, P), by computing
 * the result mod P.
 */
fun GroupContext.base64ToElementModPsafe(s: String): ElementModP =
    s.fromBase64()?.let { binaryToElementModPsafe(it) } ?: ZERO_MOD_P

/**
 * Converts a base-64 string to an [ElementModQ]. Guarantees the result is in [0, Q), by computing
 * the result mod Q.
 */
fun GroupContext.base64ToElementModQsafe(s: String): ElementModQ =
    s.fromBase64()?.let { binaryToElementModQsafe(it) } ?: ZERO_MOD_Q

/** Converts from any [Element] to a base64 string representation. */
fun Element.base64(): String = byteArray().toBase64()

/** Converts from any [Element] to a base16 (hexadecimal) string representation. */
fun Element.base16(): String = byteArray().toHex()

/** Converts an integer to an ElementModQ, with optimizations when possible for small integers */
fun Int.toElementModQ(ctx: GroupContext) =
    when {
        this < 0 -> throw NoSuchElementException("no negative numbers allowed")
        !ctx.isProductionStrength() && this >= intTestQ ->
            throw NoSuchElementException("tried to make an element >= q")
        else -> ctx.uIntToElementModQ(this.toUInt())
    }

/** Converts an integer to an ElementModQ, with optimizations when possible for small integers */
fun Int.toElementModP(ctx: GroupContext) =
    when {
        this < 0 -> throw NoSuchElementException("no negative numbers allowed")
        !ctx.isProductionStrength() && this >= intTestP ->
            throw NoSuchElementException("tried to make an element >= p")
        else -> ctx.uIntToElementModP(this.toUInt())
    }

/**
 * Returns a random number in [minimum, Q), where minimum defaults to zero. Promises to use a
 * "secure" random number generator, such that the results are suitable for use as cryptographic
 * keys.
 *
 * @throws IllegalArgumentException if the minimum is negative
 */
fun GroupContext.randomElementModQ(minimum: Int = 0) =
    binaryToElementModQsafe(randomBytes(MAX_BYTES_Q), minimum)

/**
 * We often want to raise g to small powers, for which we've conveniently pre-computed the answers.
 * This function will fall back to use [GroupContext.gPowP] if the input isn't precomputed.
 */
fun GroupContext.gPowPSmall(e: Int) =
    when {
        e == 0 -> ONE_MOD_P
        e == 1 -> G_MOD_P
        e == 2 -> G_SQUARED_MOD_P
        e < 0 -> throw ArithmeticException("not defined for negative values")
        else -> gPowP(e.toElementModQ(this))
    }

/**
 * Verifies that every element has a compatible [GroupContext] and returns the first context.
 *
 * @throws IllegalArgumentException if there's an incompatibility.
 */
fun compatibleContextOrFail(vararg elements: Element): GroupContext {
    // Engineering note: If this method fails, that means we have a bug in our program.
    // We should never allow incompatible data to be processed. We should catch
    // this when we're loading the data in the first place.

    if (elements.isEmpty()) throw IllegalArgumentException("no arguments")

    val headContext = elements[0].context

    // Note: this is comparing the head of the list to itself, which seems inefficient,
    // but adding something like drop(1) in here would allocate an ArrayList and
    // entail a bunch of copying overhead. What's here is almost certainly cheaper.
    val allCompat = elements.all { it.context.isCompatible(headContext) }

    if (!allCompat) throw IllegalArgumentException("incompatible contexts")

    return headContext
}

/**
 * Given an element x for which there exists an e, such that g^e = x, this will find e,
 * so long as e is less than [maxResult], which if unspecified defaults to a platform-specific
 * value designed not to consume too much memory (perhaps 10 million). This will consume O(e)
 * time, the first time, after which the results are memoized for all values between 0 and e,
 * for better future performance.
 *
 * If the result is not found, `null` is returned.
 */
fun ElementModP.dLogG(maxResult: Int = -1): Int? = context.dLogG(this, maxResult)

/**
 * Converts from an external [ElectionConstants] to an internal [GroupContext]. Note the optional
 * `acceleration` parameter, to specify the speed versus memory tradeoff for subsequent computation.
 * See [PowRadixOption] for details. Note that this function can return `null`, which indicates that
 * the [ElectionConstants] were incompatible with this particular library.
 */
fun ElectionConstants.toGroupContext(
    acceleration: PowRadixOption = PowRadixOption.LOW_MEMORY_USE
) : GroupContext? {
    val group4096 = productionGroup(acceleration = acceleration, mode = ProductionMode.Mode4096)
    val group3072 = productionGroup(acceleration = acceleration, mode = ProductionMode.Mode3072)

    return when {
        group4096.isCompatible(this) -> group4096
        group3072.isCompatible(this) -> group3072
        else -> {
            logger.error {
                "unrecognized cryptographic parameters; this election was encrypted using a " +
                    "library incompatible with this one: $this"
            }
            null
        }
    }
}

/**
 * Montgomery form of an [ElementModP]. Note the very limited set of methods. Convert back
 * a regular [ElementModP] for anything other than multiplication.
 */
interface MontgomeryElementModP {
    /** Modular multiplication */
    operator fun times(other: MontgomeryElementModP): MontgomeryElementModP

    /** Convert back to the normal [ElementModP] representation. */
    fun toElementModP(): ElementModP

    /** Every [MontgomeryElementModP] knows the [GroupContext] that was used to create it. */
    val context: GroupContext
}