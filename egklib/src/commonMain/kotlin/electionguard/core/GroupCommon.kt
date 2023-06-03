package electionguard.core

import electionguard.ballot.ElectionConstants
import electionguard.core.Base16.fromHex
import electionguard.core.Base16.toHex
import electionguard.core.Base64.fromBase64
import electionguard.core.Base64.toBase64
import mu.KotlinLogging
private val logger = KotlinLogging.logger("GroupCommon")

// 4096-bit P and 256-bit Q primes, plus generator G and cofactor R
// Generated from PrintConstants from the spec 1.9 hex values
internal val b64Production4096P = "AP//////////////////////////////////////////sXIX99HPeavJ47OYA/L2r0DzQyZymLYtig0XW4uq+ivnuHYgbeusmFWVUvtK+hsQ7S6uNcE4IUQnVzspEWm4JT6WyhYiSujFGsvaETF8OH656pvDsTZgOyVvoOx2V/dLcs6HsZ1lSMr136a9ODAySGVfoYcvIOOi2i2XxQ8/1cYH9MoR+1v7kGENMPiP5VGi7ladbfwe+hV9LiPeFACzlhdGB3XbiZDlyUPnMrR5zTPMzE5lk5NRTEwaHgvR1gldJWabMzVkozdqnH+KXhSOggdNtgFc/nqjDEgKVBc1DSyVXVF5seF7na4xPNtsYGyxB49zXRstsxtfULUYUGTBi00WLbOzZYU9dZihlRric+5VcLbGj5aYNJbU5tMwr4ibRKAlVHMc3I6hcpPRIopO+Y1vUXf7zwdVJopcH5U4uYJhr/1Eaxyjz16SIriMZtPFQiGD7cmUIQkLuxb689lJ8jbgKyDO6Ia5BcEo1T0L0vliE2MZavUDAgBg5JkIORoMVzObor66fQUqxbYcxOkgfO8vDOLXNzlY12ImWJBEV0T7Xy2kt1EAWJLTVokN7+nK2bnUtxPgYWKi2P3Q3y/WCP//////////////////////////////////////////"
internal val b64Production4096Q = "AP////////////////////////////////////////9D"
internal val b64Production4096P256MinusQ = "AL0="
internal val b64Production4096R = "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC8sXIX99HPeavJ47OYA/L2r0DzQyZymLYtig0XW4urhXro9CgWVBiAbGKw6jY1Wjpz4MdBmFv2oOMTAXm/LwtD4zrYYpI4YbjJ92jEFpUZYAutBgk/lksn4C2GgxIxqRYN5I9NpT2KteaeOGtpS+wa5yLUdXkknVQkdnxcM7kVHgfFwR0QasRG0zC0fbWdNS5HpTFX3gRGGQD2/jYNuJffUxbYfJSucdrQvoS2R8S8+BjCOi1Ou1PHAqXIBi0Z9em1AzqU9/9zL1QSlxKGnZe4yWxBKSGp2GeXcPSZoEHCl8/3nUyRSetsr2e56j3FY9ll86rRN3/yLenD5iBo3Q7WFRw3tPdGNMK9CdqRL9WZ9DM6jSzABWJ9yje61D5ko5YxGcC/40gQoh7nz8Qh1TOYy8epWzv1heWgS3kOL+H+m8Jk/agQn2RUoIL177LzfqI3qinfMg1uqGDEGpBUzNJIdsYlP2Z7+wE5tVMf8wGJlhIC/SsNVadScsf9czQ/eJm8oLNqTEcKZKAJJEyE53zrySQX1bsTvxgWfYAz62xN14ef1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kg=="
internal val b64Production4096G = "NgNv7SFPO1DcVm06MS/kEx/uHCvObQLqObR3rAX3+IXzjP53p+Raz0ApEUxNepv+BYvy+ZXSR5092mGP/ZENPEI2qyz914OlAW90Zc9Zu/RdJKIvEw8tBP6TstWLucHR0n/JoX0q9Jp3nz/73KIpAMFCAu5smWFgNL41y83T57t5lq3+U0tjzKQeIf9dx3jrsbhsU7++mZh9euoHViN/tAkiE5+Qpi8qqNmtNN/3meM8hXpkaNABrPO2gduH3EJCdV4qxaUCfbgZhPAzxNF4Nx8nPbtPzqHmKMI+UnWbx3ZXKANc6ia0TEmmVmaImCCkXDPdN+pKHQDLYjBc1UG+HoqSaFoHASsaIKdGw1kaLbOBUADSqsz+Q9xJ6CjB7XOHRmr9jkvxk1WTsqRC7sJxxQrTn3M3l6HqEYAqJVeRZTRmKmt+mp5EmiTIz/gJ55pNgG62gRGTMObFeYXjmyALSJNjn9/epJ92rRrNmX66E2V1QeeexXQ35QTtqd0BEGFRbGQ/sw1tWK/M0otz/top7BKwGl64Y5mlk6nV9FDeOcuSlixexpJTSNtU0Sj9mcFLRX+IPsIBEqdaagWB09gKO07wnshvlVL/2hZT8TOqJTSYOm8xsO5Gl5Naax6i91uF5+uhUbpIYJTWhyKwVGM/7FHKPymzHnfjF7F4trnYrg8="

internal val b64Production4096MontgomeryI = "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
internal val b64Production4096MontgomeryIMinus1 = "AP//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////"
internal val b64Production4096MontgomeryIPrime = "AL+bQF0cKBEdRWrPMtwSNVz+0jI2FY99DgiVSo3zl4Fh5gwIa8G31c65S6M+hnUUSyOgj+4ojFKFnyg+o+DN7yfOITXi1IJH7kxIl7itrOAloBmmMCCJbND//JJAJgT9D+EUa7NJ7552az3v+ssEjCM9JD04OJoQDwKRmxJDIdnFh9pWZhEYhohsrLIc+qRSbp8JBuIGFQcEBA5DP8N7dlh1kddTS13Nr4ksY9iylbBD6kyLPUXLQbCIGl2z4312w0KUJBHDTOmNg1fSQwTQl45O/RISEYnoM03hry+nfp2WHg/BmpvaUII7ITAchCzo1fup6RSdz2qCG2by1nRD2rAw/lpGZfoULzro+dBjEUeoZCI8rN6d0sjyech5U/gVisNcqEa4XA+ea8Q804RPon8dbJH+yERsnzy1f8Oyh36MS6dGgnjC5sMNgBMdBUfrK0jF1kNBExs6GcqTJu6zspGGbn8GDI32ic661ZndgODQlMdyq715LmHSNSmFw4wStqbhDdgN3yXb9xJvFDYRVCS1h1q4Tnhlj1EOqhHrM9NwrIsFRFTvcVPsP5V1/FxrxagYJ1acA0DQvf+xoszpPGFySTz9iXcbpMlBylr5gu4ebBSkqjYHGzrTkJGsCkqnq+JNhinTUXK8KdO0iXToyGm9jJL+LiXZ7zWtOAveShTs"
internal val b64Production4096MontgomeryPPrime = "AL+bQF0cKBEdRWrPMtwSNVz+0jI2FY99DgiVSo3zl4FiINeMNWVVKLtZ+r2mYwwz1gFDXhxzmJ6mmTUTlQ0ASUne6nH8iHPBTTxojajkcPY+99BcRU4Lqdlhdu+yI9Sq/5jEYfxz8iyx9xTeXxrBF+7JNtUN+rB0n5HBWwmwsQlDKy8kfGYkDOtSAmecyBy3PLhuUii/+Y35DDL/okjZz2xKJBu82tLFf41plk2EBW5DxGfjOwsY9J8EeZiGfFu0ozbaZMW5hlRIWCluf+d71Pf+6AAcDc9fnk7cyIydQwpgNXRkuJ2/bX5ithWlrbhFpohSCqc2dLPJciwugV0Z5FYLJAJYmWxoGl7QDbp6So0wpvaUbXgiN8q0OHlMytPx4H/Zwtl8IU5F/1/zlYWU6xkvVasvJ/J7qOp9YcsEWeXGjqGTfWPGN/s+bhWPuj6YcuQIn7sCRoaZZKoHp8J4vUQV2nNtvyRVYqO8nnX7FR2GH9CaJ+2RX0niLe+HeTeVP9tf/ohoZesFJtDhwZZMkeDnEG77L8oYYXTG9CdocQzjQE0AsmYcTf1sBBp4/YTdyI/NmXOEYZlpZ/AezIiFY5X7Xy2kt1EAWJLTVokN7+nK2bnUtxPgYWKi2P3Q3y/WCQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB"

internal const val intProduction4096PBits = 4096

// 3072-bit P and 256-bit Q primes, plus generator G and cofactor R
internal const val b64Production3072P = "AP//////////////////////////////////////////k8Rn432wx6TRvj+BAVLLVqHOzDr2XMAZDAPfNHCa/72OS1n6A6nw7tBknMtiEFfREFaukTITWgjkO0Zz10uv6ljeuHjMhtcz2+e/OBVLNs+KltFWeJmqrgwJ1Mi2t7hv0qHqHeYv+GQ+x8Jxgnl3Il5qwvC9YcdGlhVCo8476l21T+cOY+bQn4/ChljoBWekfP3mDudB5dhae9RpMc7YIgNlWUlkuDmJb8qrzMmzGVnAg/Iq0+5ZHDL6ssdEjyoFfbLbSe5S4BgnQeU4ZfAEzI5wS3xcQL8wTE2MTxPt9gR8VVMC0iONjOEd8kJPG2bCxdI40HRNtnmvKJBIcDH5wK6hxLtv6VVO5Sj98bBeWyViI7LwkhXzcZ+cfMxp3tTlMKbslAxFMU0W09hktKiTT4uHxSr6CWGgpsXuSjU3d3P//////////////////////////////////////////w=="
internal const val b64Production3072Q = "AP////////////////////////////////////////9D"
internal const val b64Production3072P256MinusQ = "AL0="
internal const val b64Production3072R = "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC8k8Rn432wx6TRvj+BAVLLVqHOzDr2XMAZDAPfNHCbivamTAztzy1VnanZfwlcMHbGhgN2GRSNLIbDFxAq+iFIAx8ERArA/wyaQXqJISUS52B7JQHapNOKLBQQxINhSeK9uMgmDmJ8RkaWPv/p4W5JXUi9IVxtjsnRZnZXoqHIUG8hE/+tGaayvHxFdgRWcZGDMJ+HS8ms5XD/2od6orI6LW8pHBVUyi6xLxLNAJuLhzSmStUeuJO9iRdQuFFiJB2QjwyXCYeXWOfoIz6rO/LWq1OvoyqhU61mguWgZIiXyb4YoNUL7OAww0MjNq2RY+M/jn2vSY8UuyhSr/qBSEHrGN1fDolRbVV3dihcFgcdIRGU7hw/NGQgNquIbj7CiWZAn9Sn9Sn9Sn9Sn9Sn9Sn9Sn9Sn9Sn9Sn9Sn9Sn9Sn9So="
internal const val b64Production3072G = "AK+NwgV5Y8bDZBGcAUonaGungFdnSLcvZwxKXUw/rB4ii4T7qIxOr5TfmHVcbHNhG7VKFKbiMtI4yRfadtimK3CDehXuwREMESVhqw6unhHdzsYfK71Uu3YvyQNJTvIfDzOP4mWCRTzj/wLFOncpYSblnhmAzUmlZyakDP3vk6GBQc+DRC0P3N+fE1Gy0M+BTOnHlkAtwiGBMtKDYFvdFUaOq6S2945N494HZvqZFe0o4A2QdX9JSYYJJHfJDF/DBaVoKQiNmW0ifS8BjBoWN3sAFKgYP1nPiHHEZZEyvdunnoaa6PZck2CNF5oH19mU4Fjl9RtHxyCaJYZNqfE3fBaxwJyFtmzD1Sf6s/ay321r6hUgYpi6w+KT8Q4um3gOzgM6R8/EUSIVIrtwnhuU2Op0hyQhhdj4+wE+nhBzldU+IsVVAvweSpFXZvPDtGOj7ky2gpJqDE+HzYYYGrxvuQK9gzHeGPWYIMXZZ9eEscBuWpTzHvhhG1RdLx4YTOrDEg=="

internal const val b64Production3072MontgomeryI = "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=="
internal const val b64Production3072MontgomeryIMinus1 = "AP///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////w=="
internal const val b64Production3072MontgomeryIPrime = "SZDM5YWGmIesvLTb/N//jdIVXxbmqBwN7XOtgqjZaIRgh2w3GyyCvV5aRepmTL4V1L7ATnbZqa6tnPKQXQJeIrtwACmZcDZT4pPwHZedGObD8Uy5EB9+vCKOpL/AY9Q3rTvcii+MGFnRUHi/+i4Sw2aex/bNWq4JCSGz5nMidI1Xs511b9oG9oczNWq5fm81iqMYDQlt80V9I8b7GbHSsSMqRYGgyistYoIjdDrKMbQrAWTcnLGq8KEKB/is9nM0CFEFJGsZstVyUykk0olqMmxP+V0suBwQCgJSDx6EkbTVheooBZTTvM5ogwLYAuYGt7+whmhkODOTyYXP88V3os+4n1amTJToqFf9XHp7iOgLST+iunzrLX9mBDjOllt7DWTLKBeygXjb5txavDfDXSeycLaPauF2zWJCl5B8/2D/cqX6TkrGAKDjEv0AB/E8MZZTLkeL1XZLw+/dCIE/YyzeSKlaiJxSZT7PaV4egUmyLOzfidO1aWeOdV1dauVc"
internal const val b64Production3072MontgomeryPPrime = "SZDM5YWGmIesvLTb/N//jdIVXxbmqBwN7XOtgqjZaIR/oaK9U1gNa/EAccwP5njLeTwbhJ7qWhQwArH9NWoCk6KT+VO7BK0aK1mMihvftnWfInDNVU1XlUwO83vF0nsQmQ7n4rJnrkJaNmroeGYlRmlHqWNufZM3eKlZoZ+PWA1OC4u0V7QgLcwottF2+lAEuLforeYMznJiMzh/44ePVonPniQDefCbfXGlNSPF8MDuLKiuz2UMYFbCCglu0QsVqLwWNgdO7pgAvSwTzcfgmuoGq9ZhpHuufKme3Fq9T7CC+GBeJQkQdwUOtpkEnrzzTneTHa/skarFatdKXWpyglyOoPEKFHLSzhR4XNFDuw1WhaHNjEGy4L+ocmzlbCLirJbWiSvBTFchbqYzLgmBvL5S41zvIRMv2WE/Vv1z4XUwpuyUDEUxTRbT2GS0qJNPi4fFKvoJYaCmxe5KNTd3dAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB"

internal const val intProduction3072PBits = 3072

// 32-bit everything, suitable for accelerated testing
internal const val intTestP = 1879047647
internal const val intTestQ = 134217689
internal const val intTestR = 14
internal const val intTestG = 16384

internal val intTestMontgomeryI = 2147483648U
internal val intTestMontgomeryIMinus1 = 2147483647U
internal val intTestMontgomeryIPrime = 1533837288U
internal val intTestMontgomeryPPrime = 1752957409U
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

interface Element : CryptoHashableString {
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

    /** Returns a string representation suitable for cryptographic hashing. */
    override fun cryptoHashString(): String = base16()
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
     * deserialize the JSON back to this type, and then use the [ElectionConstants.isCompatible]
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
    fun isCompatible(externalConstants: ElectionConstants): Boolean =
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
fun GroupContext.safeBase16ToElementModP(s: String): ElementModP =
    s.fromHex()?.let { safeBinaryToElementModP(it) } ?: ZERO_MOD_P

/**
 * Converts a base-16 (hexadecimal) string to an [ElementModQ]. Guarantees the result is in [0, Q),
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