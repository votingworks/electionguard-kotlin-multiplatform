package electionguard.core

import mu.KotlinLogging
private val logger = KotlinLogging.logger("HashedElGamal")

/**
 * The ciphertext representation of an arbitrary byte-array, encrypted with an ElGamal public key.
 */
data class HashedElGamalCiphertext(val c0: ElementModP, val c1: ByteArray, val c2: ByteArray)

/**
 * Given an array of plaintext bytes, encrypts those bytes using the "hashed ElGamal" stream cipher,
 * described in the ElectionGuard specification, in section 3. The nonce may be specified to make
 * the encryption deterministic. Otherwise, it's selected at random.
 */
fun ByteArray.hashedElGamalEncrypt(
    keypair: ElGamalKeypair,
    nonce: ElementModQ = keypair.context.randomElementModQ(minimum = 2),
): HashedElGamalCiphertext = hashedElGamalEncrypt(keypair.publicKey, nonce)

/**
 * Given an array of plaintext bytes, encrypts those bytes using the "hashed ElGamal" stream cipher,
 * described in the ElectionGuard specification, in section 3. The nonce may be specified to make
 * the encryption deterministic. Otherwise, it's selected at random.
 */
fun ByteArray.hashedElGamalEncrypt(
    key: ElGamalPublicKey,
    nonce: ElementModQ = key.context.randomElementModQ(minimum = 2)
): HashedElGamalCiphertext {
    val context = key.context
    val messageBlocks: List<ByteArray> =
        this.toList()
            .chunked(32) { block ->
                // pad each block of the message to 32 bytes
                val result = ByteArray(32) { 0 }
                block.forEachIndexed { index, byte -> result[index] = byte }
                result
            }

    // spec: (alpha, beta) = (g^R mod p, K^R mod p)
    // by encrypting a zero, we achieve exactly this
    val (alpha, beta) = 0.encrypt(key, nonce)
    val kdfKey = context.hashElements(alpha, beta).byteArray()
    val kdf = KDF(kdfKey, "", "")
    val k0 = kdf[0]
    val c0 = alpha.byteArray()
    val c1 = concatByteArrays(*messageBlocks.mapIndexed { i, p -> p xor kdf[i + 1] }.toTypedArray())
    val c2 = (c0 + c1).hmacSha256(k0)

    return HashedElGamalCiphertext(alpha, c1, c2)
}

/**
 * Attempts to decrypt the [HashedElGamalCiphertext] using the given keypair's secret key. Returns
 * `null` if the decryption fails, likely from an HMAC verification failure.
 */
fun HashedElGamalCiphertext.decrypt(keypair: ElGamalKeypair) = decrypt(keypair.secretKey)

/**
 * Attempts to decrypt the [HashedElGamalCiphertext] using the given secret key. Returns `null` if
 * the decryption fails, likely from an HMAC verification failure.
 */
fun HashedElGamalCiphertext.decrypt(secretKey: ElGamalSecretKey): ByteArray? {
    val alpha = c0
    val beta = c0 powP secretKey.key
    val kdfKey = secretKey.context.hashElements(alpha, beta).byteArray()
    val kdf = KDF(kdfKey, "", "")
    val k0 = kdf[0]

    val expectedHmac = (c0.byteArray() + c1).hmacSha256(k0)

    if (!expectedHmac.contentEquals(c2)) {
        logger.error { "HashedElGamalCiphertext decryption failure: HMAC doesn't match" }
        return null
    }

    val ciphertextBlocks = c1.toList().chunked(32) { it.toByteArray() }
    val plaintext =
        concatByteArrays(*ciphertextBlocks.mapIndexed { i, c -> c xor kdf[i + 1] }.toTypedArray())

    return plaintext
}

/**
 * NIST 800-108-compliant key derivation function (KDF) state.
 * [See the spec](https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-108.pdf),
 * section 5.1.
 *
 *  - The [key] must be 32 bytes long, suitable for use in HMAC-SHA256.
 *  - The [label] is a string that identifies the purpose for the derived keying material.
 *  - The [context] is a string containing the information related to the derived keying material.
 *    It may include identities of parties who are deriving and/or using the derived keying
 *    material.
 */
class KDF(val key: ByteArray, label: String, context: String) {
    // we're going to convert the strings as UTF-8
    val labelBytes = label.encodeToByteArray()
    val lengthBytes = (32 * 8).toByteArray()
    val contextBytes = context.encodeToByteArray()

    /** Get the requested key bits from the sequence. */
    operator fun get(index: Int): ByteArray {
        // NIST spec: K(i) := PRF (KI, [i] || Label || 0x00 || Context || [L])
        val input =
            concatByteArrays(
                index.toByteArray(),
                labelBytes,
                byteArrayOf(0),
                contextBytes,
                lengthBytes
            )
        return input.hmacSha256(key)
    }
}