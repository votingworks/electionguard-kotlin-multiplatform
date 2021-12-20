package electionguard

import kotlinx.serialization.json.JsonElement

// Only "expect" stubs should go in this file. Other shared functionality should go in
// other classes. We're trying to avoid confusion where Kotlin generates two classes
// for the JVM, both named GroupKt, which then doesn't work.

/**
 * Fetches the production-strength [GroupContext] with the desired amount of acceleration via
 * precomputation, which can result in significant extra memory usage.
 *
 * @see PowRadixOption for the different memory use vs. performance profiles
 */
expect fun productionGroup(
    acceleration: PowRadixOption = PowRadixOption.LOW_MEMORY_USE
): GroupContext

/**
 * Fetches the [GroupContext] suitable for unit tests and such, where we're still doing modular
 * arithmetic, but in 16 bits rather than 4000 bits. This will make tests much faster. **Do not use
 * this in production code!**
 */
expect fun testGroup(): GroupContext

/**
 * The GroupContext class provides all the necessary context to define the arithmetic that we'll be
 * doing, such as the moduli P and Q, the generator G, and so forth. This also allows us to
 * encapsulate acceleration data structures that we'll use to support various operations.
 */
expect class GroupContext {
    /**
     * Returns whether we're using "production primes" (bigger, slower, secure) versus "test primes"
     * (smaller, faster, but insecure).
     */
    fun isProductionStrength(): Boolean

    /**
     * Returns a JSON object which expresses the otherwise opaque state used in the GroupContext.
     * This could be useful, in conjunction with [isCompatible], to ensure that saved ballots from
     * one ElectionGuard instance are compatible with another ElectionGuard instance.
     */
    fun toJson(): JsonElement

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
     * Identifies whether the two GroupContexts are "compatible", so elements made in one context
     * would work in the other. Groups with the same primes should be compatible.
     */
    fun isCompatible(ctx: GroupContext): Boolean

    /**
     * Identifies whether the JSON representation of a GroupContext is compatible with the current
     * context.
     */
    fun isCompatible(json: JsonElement): Boolean

    /**
     * Converts a [ByteArray] to an [ElementModP]. The input array is assumed to be in big-endian
     * byte-order: the most significant byte is in the zeroth element; this is the same behavior as
     * Java's BigInteger. Guarantees the result is in [minimum, P), by computing the result mod P.
     */
    fun safeBinaryToElementModP(b: ByteArray, minimum: Int = 0): ElementModP

    /**
     * Converts a [ByteArray] to an [ElementModQ]. The input array is assumed to be in big-endian
     * byte-order: the most significant byte is in the zeroth element; this is the same behavior as
     * Java's BigInteger. Guarantees the result is in [minimum, Q), by computing the result mod Q.
     */
    fun safeBinaryToElementModQ(b: ByteArray, minimum: Int = 0): ElementModQ

    /**
     * Converts a [ByteArray] to an [ElementModP]. The input array is assumed to be in big-endian
     * byte-order: the most significant byte is in the zeroth element; this is the same behavior as
     * Java's BigInteger. Returns null if the number is out of bounds.
     */
    fun binaryToElementModP(b: ByteArray): ElementModP?

    /**
     * Converts a [ByteArray] to an [ElementModQ]. The input array is assumed to be in big-endian
     * byte-order: the most significant byte is in the zeroth element; this is the same behavior as
     * Java's BigInteger. Returns null if the number is out of bounds.
     */
    fun binaryToElementModQ(b: ByteArray): ElementModQ?

    /** Computes G^e mod p, where G is our generator */
    fun gPowP(e: ElementModQ): ElementModP

    /**
     * Computes the discrete log, base g, of p. Only yields an answer for "small" exponents,
     * otherwise returns null.
     */
    fun dLog(p: ElementModP): Int?
}

expect class ElementModQ : Element, Comparable<ElementModQ> {
    /** Modular addition */
    operator fun plus(other: ElementModQ): ElementModQ

    /** Modular subtraction */
    operator fun minus(other: ElementModQ): ElementModQ

    /** Modular multiplication */
    operator fun times(other: ElementModQ): ElementModQ

    /** Finds the multiplicative inverse */
    fun multInv(): ElementModQ

    /** Computes the additive inverse */
    operator fun unaryMinus(): ElementModQ

    /** Multiplies by the modular inverse of [denominator] */
    infix operator fun div(denominator: ElementModQ): ElementModQ

    /** Allows elements to be compared (<, >, <=, etc.) using the usual arithmetic operators. */
    override operator fun compareTo(other: ElementModQ): Int
}

expect open class ElementModP : Element, Comparable<ElementModP> {
    /**
     * Validates that this element is a quadratic residue (and is reachable from
     * [GroupContext.gPowP]). Returns true if everything is good.
     */
    fun isValidResidue(): Boolean

    /** Computes b^e mod p */
    open infix fun powP(e: ElementModQ): ElementModP

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
    open fun acceleratePow(): ElementModP
}

/**
 * Computes the sum of the given elements, mod q; this can be faster than using the addition
 * operation for large numbers of inputs by potentially reusing scratch-space memory.
 */
expect fun Iterable<ElementModQ>.addQ(): ElementModQ

/**
 * Computes the product of the given elements, mod p; this can be faster than using the
 * multiplication operation for large numbers of inputs by potentially reusing scratch-space memory.
 */
expect fun Iterable<ElementModP>.multP(): ElementModP

/** Converts an integer to an ElementModQ, with optimizations when possible for small integers */
expect fun UInt.toElementModQ(ctx: GroupContext) : ElementModQ

/** Converts an integer to an ElementModP, with optimizations when possible for small integers */
expect fun UInt.toElementModP(ctx: GroupContext) : ElementModP
