package electionguard.core

import electionguard.core.Base16.fromHex
import electionguard.core.Base16.toHex
import electionguard.core.Base64.fromBase64
import electionguard.core.Base64.toBase64
import electionguard.publish.Constants
import electionguard.publish.isCompatible

// 4096-bit P and 256-bit Q primes, plus generator G and cofactor R
internal val b64ProductionP =
    "AP//////////////////////////////////////////k8Rn432wx6TRvj+BAVLLVqHOzDr2XMAZDAPfNHCa/72OS1n6A6nw7tBknMtiEFfREFaukTITWgjkO0Zz10uv6ljeuHjMhtcz2+e/OBVLNs+KltFWeJmqrgwJ1Mi2t7hv0qHqHeYv+GQ+x8Jxgnl3Il5qwvC9YcdGlhVCo8476l21T+cOY+bQn4/ChljoBWekfP3mDudB5dhae9RpMc7YIgNlWUlkuDmJb8qrzMmzGVnAg/Iq0+5ZHDL6ssdEjyoFfbLbSe5S4BgnQeU4ZfAEzI5wS3xcQL8wTE2MTxPt9gR8VVMC0iONjOEd8kJPG2bCxdI40HRNtnmvKJBIcDH5wK6hxLtv6VVO5Sj98bBeWyViI7LwkhXzcZ+cfMxp3fFy0NYjQhf8wAN/GLk+9TiRMLemYeXCblQhQGi7yv6jKmeBi9MHWtH1x+nMPRc3+ygXG6+E27ZhK3iBwaSOQ5zQOpK/UiJaKzjmVC6fcivOFaOBtXU+qEJ2M4HMroNRKzBRGzLl6NgDYhSa0DCqul86V5i7Iqp+wbbQ8XkD9OIthAc0qoWXP3mpP/uCp1xHwD1D0vnKAtAxmbrO3dRTOlJWav//////////////////////////////////////////"
internal val b64ProductionQ = "AP////////////////////////////////////////9D"
internal val b64ProductionP256MinusQ = "AL0="
internal val b64ProductionR =
    "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC8k8Rn432wx6TRvj+BAVLLVqHOzDr2XMAZDAPfNHCbivamTAztzy1VnanZfwlcMHbGhgN2GRSNLIbDFxAq+iFIAx8ERArA/wyaQXqJISUS52B7JQHapNOKLBQQxINhSeK9uMgmDmJ8RkaWPv/p4W5JXUi9IVxtjsnRZnZXoqHIUG8hE/+tGaayvHxFdgRWcZGDMJ+HS8ms5XD/2od6orI6LW8pHBVUyi6xLxLNAJuLhzSmStUeuJO9iRdQuFFiJB2QjwyXCYeXWOfoIz6rO/LWq1OvoyqhU61mguWgZIiXyb4YoNUL7OAww0MjNq2RY+M/jn2vSY8UuyhSr/qBSEHrGN1fDolRbVV3dihcFgcdIRGU7hw/NGQgNquIbj7CiILOQAPeozW02TW65LWCNbn7K6txPI9wWhx95CIgIJ1rvKzEZzGGAVZScuSmPjjiSZdUrkk6wajoNGnu81yifCcbx5Lu4hFW5he5IuqPcTwizygtxdY4W7EoaOt4Enj6CrKolY/Mtf/i5cNh/BdEIBIrAWPKSkYwjIxGyR6nRXwTan2f1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kg=="
internal val b64ProductionG =
    "HUHknEd+Feru8MXkrAjUpGwmjNNCT8AdE3ab20NnMhhYe8hsTBRI0AagNpnzq65f6xnilvXRQ8xeSj/IkIjJ9FI9Fm7jrp1fsDwL3Xet1cAX9sVeLsksIm/vXGwd8ufDbZDn6q3gmCQdNAmYO8zStTeek5H7xi+fjZOdEgixYDZ8E0JkEiGJWV7IXIzb5fnTB/RpEsBJMvjBaBWna0aCvWvcDtUrANjTD1nHMdWn/66BZdU8+WZJqsK3Q9pW8U8Z2sxSNvKbGrn5vvxpaXKT1d6ti1v13purbeZ8RXGeVjRKPL3zYJgksbV4406utt0xkKs1cdbWccUSKCwdp702tCUdJYT63qgLnhQUIwdN2bX7g6y96tTIelj/9Rf5d6gwgDcKOwz5ihvCl4xHqsKWEf1sQOL5h1w11QRDqao/SWEdzToNb/PLP6zzFHG9thhguSxZTU5GVpuzn+6t/x/WTINqbW24XGunJBdmt6tWv3OWM7BUFH9xcJIUEulI2eR0AtFbscJXMYYSwSHDa4DrhDPAjn0LcUnjqwqHNaku3Oj/lD4ootzqz8xp7DGJCcsEe+HFhYhEta1E8i7rKJ5MxVT3peLz3qAmh3/5KFGBYHHOAo64aNllzLLSKVqMVb0cBws5sJrgazfSk0O52Jl9wkTEaLmAlwcxc27gGLutuYc="

// 32-bit everything, suitable for accelerated testing
internal val b64TestP = "b//93w=="
internal val b64TestQ = "B///2Q=="
internal val b64TestP256MinusQ = "AP/////////////////////////////////////4AAAn"
internal val b64TestR = "Dg=="
internal val b64TestG = "QAA="

// useful for checking the decoders from base64
internal const val intTestP = 1879047647
internal const val intTestQ = 134217689
internal const val intTestR = 14
internal const val intTestG = 16384

interface Element {
    /**
     * Every Element knows the [GroupContext] that was used to create it. This simplifies code that
     * computes with elements, allowing arithmetic expressions to be written in many cases without
     * needing to pass in the context.
     */
    val context: GroupContext
        get

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

    /** Converts from any [Element] to a compact [ByteArray] representation. */
    fun byteArray(): ByteArray
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
     * deserialize the JSON back to this type, and then use the [Constants.isCompatible] method to
     * validate that external data is compatible with internal values.
     */
    val constants: Constants

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
     * Identifies whether two internal GroupContexts are "compatible", so elements made in one
     * context would work in the other. Groups with the same primes will be compatible. Note that
     * this is meant to be fast, so only makes superficial checks. The [Constants] variant of this
     * method validates that all the group constants are the same.
     */
    fun isCompatible(ctx: GroupContext): Boolean

    /** Identifies whether an external [Constants] is "compatible" with this GroupContext. */
    fun isCompatible(externalConstants: Constants): Boolean =
        constants.isCompatible(externalConstants)

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
     * The `maxQMinus1` flag changes the range to [minimum, Q-1), which is used for compatibility
     * with the original ElectionGuard hash function.
     */
    fun safeBinaryToElementModQ(
        b: ByteArray,
        minimum: Int = 0,
        maxQMinus1: Boolean = false
    ): ElementModQ

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
     * Computes the discrete log, base g, of p. Only yields an answer for "small" exponents,
     * otherwise returns null.
     */
    fun dLog(p: ElementModP): Int?
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

    /** Computes b^e mod q */
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
     * Validates that this element is a quadratic residue (and is thus reachable from
     * [GroupContext.gPowP]). Returns true if everything is good.
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
 * Converts a base-16 (hexidecimal) string to an [ElementModP]. Returns null if the number is out of
 * bounds or the string is malformed.
 */
fun GroupContext.base16ToElementModP(s: String): ElementModP? =
    s.fromHex()?.let { binaryToElementModP(it) }

/**
 * Converts a base-16 (hexidecimal) string to an [ElementModQ]. Returns null if the number is out of
 * bounds or the string is malformed.
 */
fun GroupContext.base16ToElementModQ(s: String): ElementModQ? =
    s.fromHex()?.let { binaryToElementModQ(it) }

/**
 * Converts a base-16 (hexidecimal) string to an [ElementModP]. Guarantees the result is in [0, P),
 * by computing the result mod P.
 */
fun GroupContext.safeBase16ToElementModP(s: String): ElementModP =
    s.fromHex()?.let { safeBinaryToElementModP(it) } ?: ZERO_MOD_P

/**
 * Converts a base-16 (hexidecimal) string to an [ElementModQ]. Guarantees the result is in [0, Q),
 * by computing the result mod Q.
 */
fun GroupContext.safeBase16ToElementModQ(s: String): ElementModQ =
    s.fromHex()?.let { safeBinaryToElementModQ(it) } ?: ZERO_MOD_Q

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
fun GroupContext.safeBase64ToElementModP(s: String): ElementModP =
    s.fromBase64()?.let { safeBinaryToElementModP(it) } ?: ZERO_MOD_P

/**
 * Converts a base-64 string to an [ElementModQ]. Guarantees the result is in [0, Q), by computing
 * the result mod Q.
 */
fun GroupContext.safeBase64ToElementModQ(s: String): ElementModQ =
    s.fromBase64()?.let { safeBinaryToElementModQ(it) } ?: ZERO_MOD_Q

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
    safeBinaryToElementModQ(randomBytes(MAX_BYTES_Q), minimum)

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
    if (elements.isEmpty()) throw IllegalArgumentException("no arguments")

    val headContext = elements[0].context

    // Note: this is comparing the head of the list to itself, which seems inefficient,
    // but adding something like drop(1) in here would allocate an ArrayList and
    // entail a bunch of copying overhead. What's here is almost certainly cheaper.
    val allCompat = elements.all { it.context.isCompatible(headContext) }

    if (!allCompat) throw IllegalArgumentException("incompatible contexts")

    return headContext
}