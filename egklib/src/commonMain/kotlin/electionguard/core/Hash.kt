package electionguard.core

import io.ktor.utils.io.core.*

/**
 * Any ElectionGuard type can implement this interface, and is then supported by [hashElements]. If
 * multiple CryptoHashable interfaces are implemented, [CryptoHashableString] is the highest
 * priority, followed by [CryptoHashableUInt256], and lastly [CryptoHashableElement].
 */
interface CryptoHashableString {
    /** Returns a string suitable for input to a cryptographic hash function. */
    fun cryptoHashString(): String
}

/**
 * Any ElectionGuard type can implement this interface, and is then supported by [hashElements]. If
 * multiple CryptoHashable interfaces are implemented, [CryptoHashableString] is the highest
 * priority, followed by [CryptoHashableUInt256], and lastly [CryptoHashableElement].
 */
interface CryptoHashableUInt256 {
    /** Returns a [UInt256], suitable for input to a cryptographic hash function. */
    fun cryptoHashUInt256(): UInt256
}

/**
 * Any ElectionGuard type can implement this interface, and is then supported by [hashElements]. If
 * multiple CryptoHashable interfaces are implemented, [CryptoHashableString] is the highest
 * priority, followed by [CryptoHashableUInt256], and lastly [CryptoHashableElement].
 */
interface CryptoHashableElement {
    /** Returns an [Element], suitable for input to a cryptographic hash function. */
    fun cryptoHashElement(): Element
}

/**
 * The hash function H used in ElectionGuard is HMAC-SHA-256, i.e. HMAC instantiated with SHA-25639 .
 * Therefore, H takes two byte arrays as inputs.
 *
 * The first input corresponds to the key in HMAC. The HMAC-SHA-256 specification allows arbitrarily long keys,
 * but preprocesses the key to make it exactly 64 bytes (512 bits) long, which is the block size for the input to the
 * SHA-256 hash function. If the given key is smaller, HMAC-SHA-256 pads it with 00 bytes at the end, if it is longer,
 * HMAC-SHA-256 hashes it first with SHA-256 and then pads it to 64 bytes. In ElectionGuard, all inputs that are
 * used as the HMAC key, i.e. all inputs to the first argument of H have a fixed length of exactly 32 bytes.
 *
 * The second input can have arbitrary length and is only restricted by the maximal input length
 * for SHA-256 and HMAC. Hence we view the function H formally as follows (understanding that
 * HMAC implementations pad the 32-byte keys to exactly 64 bytes by appending 32 00 bytes):
 *          H : B^32 × B* → B^32 , H(B0 ; B1 ) → HMAC-SHA-256(B0 , B1 )       spec 1.9 eq 104
 *
 * Given zero or more elements, calculate their cryptographic hash using SHA256. Specifically
 * handled types are [Element], [String], and [Iterable] containers of those types, as well as
 * anything implementing [CryptoHashableString] or [CryptoHashableElement]. Unsupported types yield
 * an [IllegalArgumentException].
 *
 * Of course, infinitely long iterables cannot be hashed, and will cause this function to run
 * forever or until it runs out of memory.
 *
 * @param elements Zero or more elements of any of the accepted types.
 * @return A cryptographic hash of these elements, converted to strings and suitably concatenated.
 */
fun hashFunction(key: ByteArray, vararg elements: Any): UInt256 {
    val hmac = HmacSha256(key)
    elements.forEach { hmac.update(hashElementsToByteArray(it)) }
    return hmac.finish()
}

private fun hashElementsToByteArray(element : Any) : ByteArray {
    return when (element) {
        is Byte -> ByteArray(1) { element }
        is ByteArray -> element
        is UInt256 -> element.bytes
        is Element -> element.byteArray()
        // is CryptoHashableString -> element.cryptoHashString().toByteArray()
        // is CryptoHashableUInt256 -> element.cryptoHashUInt256().cryptoHashString().toByteArray()
        is String -> element.toByteArray()
        is Short -> hashElementsToByteArray(element.toUShort())
        is UShort -> ByteArray(2) { if (it == 0) (element / U256).toByte() else (element % U256).toByte() }
        else -> throw IllegalArgumentException("unknown type in hashElements: ${element::class}")
    }
}

private val U256 = 256.toUShort()

fun hashFunctionConcat(key: ByteArray, vararg elements: Any): UInt256 {
    var result = ByteArray(0)
    elements.forEach { result = result + hashElementsToByteArray(it) }
    val hmac = HmacSha256(key)
    hmac.update(result)
    println("size = ${result.size}")
    return hmac.finish()
}

fun hashFunctionConcatSize(key: ByteArray, vararg elements: Any): Int {
    var result = ByteArray(0)
    elements.forEach {
        val eh = hashElementsToByteArray(it)
        println("  size = ${eh.size}")
        result = result + eh
    }
    return result.size
}

//////////////////////////// OLD

/** Wrapper class to serve as an HMAC-SHA256 machine for the given key. */
class HmacProcessor(private val hmacKey: UInt256) {
    /**
     * Given zero or more elements, calculate their cryptographic HMAC using HMAC-SHA256, with the
     * given [hmacKey]. Specifically handled types are [Element], [String], and [Iterable]
     * containers of those types, as well as anything implementing [CryptoHashableString] or
     * [CryptoHashableElement]. Unsupported types yield an [IllegalArgumentException].
     *
     * Of course, infinitely long iterables cannot be hashed, and will cause this function to run
     * forever or until it runs out of memory.
     *
     * @param elements Zero or more elements of any of the accepted types.
     * @return A cryptographic HMAC of these elements, converted to strings and suitably
     *     concatenated.
     */
    fun hmacElements(vararg elements: Any?): UInt256 = hmacElementsHelper(elements)

    private fun hmacElementsHelper(elements: Array<out Any?>): UInt256 =
        hashElementsHelper({ hmacElementsHelper(it) }, { it.hmacSha256(hmacKey) }, elements)
}

/**
 * Given zero or more elements, calculate their cryptographic hash using SHA256. Specifically
 * handled types are [Element], [String], and [Iterable] containers of those types, as well as
 * anything implementing [CryptoHashableString] or [CryptoHashableElement]. Unsupported types yield
 * an [IllegalArgumentException].
 *
 * Of course, infinitely long iterables cannot be hashed, and will cause this function to run
 * forever or until it runs out of memory.
 *
 * @param elements Zero or more elements of any of the accepted types.
 * @return A cryptographic hash of these elements, converted to strings and suitably concatenated.
 */
fun hashElements(vararg elements: Any?): UInt256 = hashElementsHelper(elements)

private fun hashElementsHelper(elements: Array<out Any?>): UInt256 =
    hashElementsHelper({ hashElementsHelper(it) }, { it.sha256() }, elements)

private fun hashElementsHelper(
    recursive: (Array<out Any?>) -> UInt256,
    byteHash: (ByteArray) -> UInt256,
    elements: Array<out Any?>
): UInt256 {
    val hashMe =
        elements.joinToString(prefix = "|", separator = "|", postfix = "|") {
            when (it) {
                null -> "null"
                is CryptoHashableString -> it.cryptoHashString()
                is CryptoHashableUInt256 -> it.cryptoHashUInt256().cryptoHashString()
                is CryptoHashableElement -> it.cryptoHashElement().cryptoHashString()
                is String -> it
                is Number, UInt, ULong, UShort, UByte -> it.toString()
                is Iterable<*> ->
                    // The simplest way to deal with lists and such are to crunch them
                    // recursively.

                    // We special-case the empty list, because it hashes to "null".
                    // Note that empty lists will hopefully never occur in practice,
                    // anywhere in ElectionGuard. Nulls should be uncommon, but might
                    // show up in the various data structures representing ballots,
                    // candidates, and so forth, which have optional fields.

                    if (it.none())
                        "null"
                    else
                        recursive(it.toList().toTypedArray()).cryptoHashString()
                else -> throw IllegalArgumentException("unknown type in hashElements: ${it::class}")
            }
        }

    // println("  hashAll: $hashMe")
    return byteHash(hashMe.encodeToByteArray())
}

// TODO: do we need to be able to hash anything else that wouldn't already be covered
//   by CryptoHashable? The Python reference code, if it sees an unknown type, just
//   calls str() on it, getting whatever Python implementation-specific string representation
//   might be present. For contrast, we throw an exception here. This might at least
//   become a useful way to detect things that need to be CryptoHashable but aren't.
//   At the very least, any code that doesn't know what to do can simply implement
//   CryptoHashableString and return the value of a call to toString(). It's probably
//   a desirable engineering goal for all of this to be explicitly specified, rather
//   than quietly using toString(), yielding undefined or unexpected behavior.