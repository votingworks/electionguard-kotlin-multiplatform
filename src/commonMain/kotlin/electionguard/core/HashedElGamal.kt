package electionguard.core

import mu.KotlinLogging
private val logger = KotlinLogging.logger("HashedElGamal")

/**
 * The ciphertext representation of an arbitrary byte-array, encrypted with an ElGamal public key.
 */
data class HashedElGamalCiphertext(
    val c0: ElementModP,
    val c1: ByteArray,
    val c2: UInt256,
    val numBytes: Int
)

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
    val messageBlocks: List<UInt256> =
        this.toList()
            .chunked(32) { block ->
                // pad each block of the message to 32 bytes
                val result = ByteArray(32) { 0 }
                block.forEachIndexed { index, byte -> result[index] = byte }
                UInt256(result)
            }

    // ElectionGuard spec: (alpha, beta) = (g^R mod p, K^R mod p)
    // by encrypting a zero, we achieve exactly this
    val (alpha, beta) = 0.encrypt(key, nonce)
    val kdfKey = hashElements(alpha, beta)

    // NIST spec: the length is the size of the message in *bits*, but that's annoying
    // to use anywhere else, so we're otherwise just tracking the size in bytes.
    val kdf = KDF(kdfKey, "", "", size * 8)
    val k0 = kdf[0]
    val c0 = alpha.byteArray()
    val encryptedBlocks =
        messageBlocks.mapIndexed { i, p -> (p xor kdf[i + 1]).bytes }.toTypedArray()
    val c1 = concatByteArrays(*encryptedBlocks)
    val c2 = (c0 + c1).hmacSha256(k0)

    return HashedElGamalCiphertext(alpha, c1, c2, size)
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
    val kdfKey = hashElements(alpha, beta)
    val kdf = KDF(kdfKey, "", "", numBytes * 8)
    val k0 = kdf[0]

    val expectedHmac = (c0.byteArray() + c1).hmacSha256(k0)

    if (expectedHmac != c2) {
        logger.error { "HashedElGamalCiphertext decryption failure: HMAC doesn't match" }
        return null
    }

    val ciphertextBlocks = c1.toList().chunked(32) { it.toByteArray().toUInt256() }
    val plaintextBlocks =
        ciphertextBlocks.mapIndexed { i, c -> (c xor kdf[i + 1]).bytes }.toTypedArray()
    val plaintext = concatByteArrays(*plaintextBlocks)

    return if (plaintext.size == numBytes) {
        plaintext
    } else {
        // Truncate trailing values, which should be zeros. No need to check, because
        // we've already validated the HMAC on the data.
        plaintext.copyOfRange(0, numBytes)
    }
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
 *  - The [length] specifies the length of the encrypted message in *bits*, not bytes.
 */
class KDF(val key: UInt256, label: String, context: String, length: Int) {
    // we're going to convert the strings as UTF-8
    val labelBytes = label.encodeToByteArray()
    val lengthBytes = length.toByteArray()
    val contextBytes = context.encodeToByteArray()

    /** Get the requested key bits from the sequence. */
    operator fun get(index: Int): UInt256 {
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