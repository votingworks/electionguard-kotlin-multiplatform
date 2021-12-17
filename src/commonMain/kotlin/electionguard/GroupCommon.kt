package electionguard

import com.soywiz.krypto.encoding.*


// 4096-bit P and 256-bit Q primes, plus generator G and cofactor R
internal val b64ProductionP =
    "AP//////////////////////////////////////////k8Rn432wx6TRvj+BAVLLVqHOzDr2XMAZDAPfNHCa/72OS1n6A6nw7tBknMtiEFfREFaukTITWgjkO0Zz10uv6ljeuHjMhtcz2+e/OBVLNs+KltFWeJmqrgwJ1Mi2t7hv0qHqHeYv+GQ+x8Jxgnl3Il5qwvC9YcdGlhVCo8476l21T+cOY+bQn4/ChljoBWekfP3mDudB5dhae9RpMc7YIgNlWUlkuDmJb8qrzMmzGVnAg/Iq0+5ZHDL6ssdEjyoFfbLbSe5S4BgnQeU4ZfAEzI5wS3xcQL8wTE2MTxPt9gR8VVMC0iONjOEd8kJPG2bCxdI40HRNtnmvKJBIcDH5wK6hxLtv6VVO5Sj98bBeWyViI7LwkhXzcZ+cfMxp3fFy0NYjQhf8wAN/GLk+9TiRMLemYeXCblQhQGi7yv6jKmeBi9MHWtH1x+nMPRc3+ygXG6+E27ZhK3iBwaSOQ5zQOpK/UiJaKzjmVC6fcivOFaOBtXU+qEJ2M4HMroNRKzBRGzLl6NgDYhSa0DCqul86V5i7Iqp+wbbQ8XkD9OIthAc0qoWXP3mpP/uCp1xHwD1D0vnKAtAxmbrO3dRTOlJWav//////////////////////////////////////////"
internal val b64ProductionQ = "AP////////////////////////////////////////9D"
internal val b64ProductionP256MinusQ = "AL0="
internal val b64ProductionR =
    "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC8k8Rn432wx6TRvj+BAVLLVqHOzDr2XMAZDAPfNHCbivamTAztzy1VnanZfwlcMHbGhgN2GRSNLIbDFxAq+iFIAx8ERArA/wyaQXqJISUS52B7JQHapNOKLBQQxINhSeK9uMgmDmJ8RkaWPv/p4W5JXUi9IVxtjsnRZnZXoqHIUG8hE/+tGaayvHxFdgRWcZGDMJ+HS8ms5XD/2od6orI6LW8pHBVUyi6xLxLNAJuLhzSmStUeuJO9iRdQuFFiJB2QjwyXCYeXWOfoIz6rO/LWq1OvoyqhU61mguWgZIiXyb4YoNUL7OAww0MjNq2RY+M/jn2vSY8UuyhSr/qBSEHrGN1fDolRbVV3dihcFgcdIRGU7hw/NGQgNquIbj7CiILOQAPeozW02TW65LWCNbn7K6txPI9wWhx95CIgIJ1rvKzEZzGGAVZScuSmPjjiSZdUrkk6wajoNGnu81yifCcbx5Lu4hFW5he5IuqPcTwizygtxdY4W7EoaOt4Enj6CrKolY/Mtf/i5cNh/BdEIBIrAWPKSkYwjIxGyR6nRXwTan2f1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kg=="
internal val b64ProductionG =
    "HUHknEd+Feru8MXkrAjUpGwmjNNCT8AdE3ab20NnMhhYe8hsTBRI0AagNpnzq65f6xnilvXRQ8xeSj/IkIjJ9FI9Fm7jrp1fsDwL3Xet1cAX9sVeLsksIm/vXGwd8ufDbZDn6q3gmCQdNAmYO8zStTeek5H7xi+fjZOdEgixYDZ8E0JkEiGJWV7IXIzb5fnTB/RpEsBJMvjBaBWna0aCvWvcDtUrANjTD1nHMdWn/66BZdU8+WZJqsK3Q9pW8U8Z2sxSNvKbGrn5vvxpaXKT1d6ti1v13purbeZ8RXGeVjRKPL3zYJgksbV4406utt0xkKs1cdbWccUSKCwdp702tCUdJYT63qgLnhQUIwdN2bX7g6y96tTIelj/9Rf5d6gwgDcKOwz5ihvCl4xHqsKWEf1sQOL5h1w11QRDqao/SWEdzToNb/PLP6zzFHG9thhguSxZTU5GVpuzn+6t/x/WTINqbW24XGunJBdmt6tWv3OWM7BUFH9xcJIUEulI2eR0AtFbscJXMYYSwSHDa4DrhDPAjn0LcUnjqwqHNaku3Oj/lD4ootzqz8xp7DGJCcsEe+HFhYhEta1E8i7rKJ5MxVT3peLz3qAmh3/5KFGBYHHOAo64aNllzLLSKVqMVb0cBws5sJrgazfSk0O52Jl9wkTEaLmAlwcxc27gGLutuYc="

// 16-bit everything, suitable for accelerated testing
internal val b64TestP = "AP7z"
internal val b64TestQ = "f3k="
internal val b64Test256MinusQ = "AP///////////////////////////////////////4CH"
internal val b64TestG = "Aw=="
internal val b64TestR = "Ag=="

// useful for checking the decoders from base64
internal val intTestP = 65267
internal val intTestQ = 32633
internal val intTestG = 3
internal val intTestR = 2

/**
 * Computes the sum of the given elements, mod q; this can be faster than using the addition
 * operation for large numbers of inputs by potentially reusing scratch-space memory.
 */
fun addQ(vararg elements: ElementModQ) = elements.asIterable().addQ()

/**
 * Computes the product of the given elements, mod p; this can be faster than using the
 * multiplication operation for large numbers of inputs by potentially reusing scratch-space memory.
 */
fun multP(vararg elements: ElementModP) = elements.asIterable().multP()

/**
 * Converts a base-64 string to an [ElementModP]. Returns null if the number is out of bounds or the
 * string is malformed.
 */
fun GroupContext.base64ToElementModP(s: String): ElementModP? =
    binaryToElementModP(s.fromBase64())

/**
 * Converts a base-64 string to an [ElementModP]. Guarantees the result is in [0, P), by computing
 * the result mod P.
 */
fun GroupContext.safeBase64ToElementModP(s: String): ElementModP =
    safeBinaryToElementModP(s.fromBase64())

/**
 * Converts a base-64 string to an [ElementModQ]. Guarantees the result is in [0, Q), by computing
 * the result mod Q.
 */
fun GroupContext.safeBase64ToElementModQ(s: String): ElementModQ =
    safeBinaryToElementModQ(s.fromBase64())

/**
 * Converts a base-64 string to an [ElementModQ]. Returns null if the number is out of bounds or the
 * string is malformed.
 */
fun GroupContext.base64ToElementModQ(s: String): ElementModQ? =
    binaryToElementModQ(s.fromBase64())

/** Converts from any [Element] to a base64 string representation. */
fun Element.base64(): String = byteArray().toBase64()

/** Converts an integer to an ElementModQ, with optimizations when possible for small integers */
fun Int.toElementModQ(ctx: GroupContext) =
    if (this < 0)
        throw NoSuchElementException("no negative numbers allowed")
    else
        this.toUInt().toElementModQ(ctx)

/** Converts an integer to an ElementModQ, with optimizations when possible for small integers */
fun Int.toElementModP(ctx: GroupContext) =
    if (this < 0)
        throw NoSuchElementException("no negative numbers allowed")
    else
        this.toUInt().toElementModP(ctx)

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

    /** Converts from any [Element] to a compact [ByteArray] representation. */
    fun byteArray(): ByteArray
}

/**
 * Returns a random number in [minimum, Q), where minimum defaults to zero. Promises to use a
 * "secure" random number generator, such that the results are suitable for use as cryptographic
 * keys.
 *
 * @throws GroupException if the minimum is negative
 */
fun GroupContext.randomElementModQ(minimum: Int = 0) =
    safeBinaryToElementModQ(randomBytes(32), minimum)

/**
 * We often want to raise g to small powers, for which we've conveniently pre-computed
 * the answers. This function will back out and use [GroupContext.gPowP] if the input
 * isn't precomputed.
 */
fun GroupContext.gPowPSmall(e: Int) = when {
    e == 0 -> ONE_MOD_P
    e == 1 -> G_MOD_P
    e == 2 -> G_SQUARED_MOD_P
    e < 0 -> throw ArithmeticException("not defined for negative values")
    else -> gPowP(e.toElementModQ(this))
}