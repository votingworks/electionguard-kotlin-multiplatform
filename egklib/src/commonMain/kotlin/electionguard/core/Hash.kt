package electionguard.core

import io.ktor.utils.io.core.*

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
 *    H : B^32 × B* → B^32 , H(B0 ; B1 ) → HMAC-SHA-256(B0 , B1 )  ; spec 1.9 eq 104
 *
 * @param key HMAC key.
 * @param elements Zero or more elements of any of the accepted types.
 * @return A cryptographic hash of these elements, converted to strings and suitably concatenated.
 */
fun hashFunction(key: ByteArray, vararg elements: Any): UInt256 {
    val hmac = HmacSha256(key)
    elements.forEach { hmac.addToHash(it) }
    return hmac.finish()
}

// identical to hashFunction, made separate to follow the spec.
fun hmacFunction(key: ByteArray, vararg elements: Any): UInt256 {
    val hmac = HmacSha256(key)
    elements.forEach { hmac.addToHash(it) }
    return hmac.finish()
}

private fun HmacSha256.addToHash(element : Any) {
    if (element is Iterable<*>) {
        element.forEach { this.addToHash(it!!) }
    } else {
        val ba : ByteArray = when (element) {
            is Byte -> ByteArray(1) { element }
            is ByteArray -> element
            is UInt256 -> element.bytes
            is Element -> element.byteArray()
            is String -> element.toByteArray()
            is Short -> ByteArray(2) { if (it == 0) (element / 256).toByte() else (element % 256).toByte() }
            is UShort -> ByteArray(2) { if (it == 0) (element / U256).toByte() else (element % U256).toByte() }
            is Int -> numberToByteArray(element as Number, 4) // only used in Nonces
            else -> throw IllegalArgumentException("unknown type in hashElements: ${element::class}")
        }
        this.update(ba)
    }
}

private val U256 = 256.toUShort()

//// test concatenation vs update

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

private fun hashElementsToByteArray(element : Any) : ByteArray {
    if (element is Iterable<*>) {
        var result = ByteArray(0)
        element.forEach { result = result + hashElementsToByteArray(it!!) }
        return result
    } else {
        val ba : ByteArray = when (element) {
            is Byte -> ByteArray(1) { element }
            is ByteArray -> element
            is UInt256 -> element.bytes
            is Element -> element.byteArray()
            is String -> element.toByteArray()
            is Short -> ByteArray(2) { if (it == 0) (element / 256).toByte() else (element % 256).toByte() }
            is UShort -> ByteArray(2) { if (it == 0) (element / U256).toByte() else (element % U256).toByte() }
            is Int -> numberToByteArray(element as Number, 4)
            else -> throw IllegalArgumentException("unknown type in hashElements: ${element::class}")
        }
        return ba
    }
}

private fun numberToByteArray (data: Number, size: Int = 4) : ByteArray =
    ByteArray (size) {i -> (data.toLong() shr (i*8)).toByte()}


////////////////////////////// KDF

/**
 * NIST 800-108-compliant key derivation function (KDF) state.
 * [See the spec](https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-108.pdf),
 * section 5.1.
 *
 *  - The [key] must be 32 bytes long, suitable for use in HMAC-SHA256.
 *  - The [label] is a string that identifies the purpose for the derived keying material.
 *  - The [context] is a string containing the information related to the derived keying material.
 *    It may include identities of parties who are deriving and/or using the derived keying material.
 *  - The [lengthInBits] specifies the length of the encrypted message in *bits*, not bytes.
 */
class KDF(val key: UInt256, label: String, context: String, lengthInBits: Int) {
    // we're going to convert the strings as UTF-8
    private val labelBytes = label.encodeToByteArray()
    private val contextBytes = context.encodeToByteArray()
    private val lengthInBitsByteArray = lengthInBits.toByteArray()

    /** Get the requested key bits from the sequence. */
    operator fun get(index: Int): UInt256 {
        // NIST spec: K(i) := PRF (KI, [i] || Label || 0x00 || Context || [L])
        val input =
            concatByteArrays(
                index.toByteArray(),
                labelBytes,
                byteArrayOf(0),
                contextBytes,
                lengthInBitsByteArray
            )
        return input.hmacSha256(key)
    }
}
