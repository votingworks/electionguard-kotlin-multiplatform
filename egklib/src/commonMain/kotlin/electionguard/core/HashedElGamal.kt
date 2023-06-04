package electionguard.core

import mu.KotlinLogging
private val logger = KotlinLogging.logger("HashedElGamal")

/**
 * The ciphertext representation of an arbitrary byte-array, encrypted with an ElGamal public key.
 * spec 1.9, section 3.2.2 eq 17.
 */
data class HashedElGamalCiphertext(
    val c0: ElementModP,
    val c1: ByteArray,
    val c2: UInt256,
    val numBytes: Int
) {
    // override because of the ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as HashedElGamalCiphertext

        if (c0 != other.c0) return false
        if (!c1.contentEquals(other.c1)) return false
        if (c2 != other.c2) return false
        if (numBytes != other.numBytes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = c0.hashCode()
        result = 31 * result + c1.contentHashCode()
        result = 31 * result + c2.hashCode()
        result = 31 * result + numBytes
        return result
    }
}

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
    val kdf = KDF(kdfKey, "", "", this.size * 8)
    val k0 = kdf[0]
    val c0 = alpha.byteArray()
    val encryptedBlocks =
        messageBlocks.mapIndexed { i, p -> (p xor kdf[i + 1]).bytes }.toTypedArray()
    val c1 = concatByteArrays(*encryptedBlocks)
    val c2 = (c0 + c1).hmacSha256(k0)

    return HashedElGamalCiphertext(alpha, c1, c2, this.size)
}

/**
 * Decrypt the [HashedElGamalCiphertext] using the given secret key. Returns `null` if
 * the decryption fails, likely from an HMAC verification failure.
 */
fun HashedElGamalCiphertext.decrypt(secretKey: ElGamalSecretKey): ByteArray? =
    decryptAlphaBeta(c0, c0 powP secretKey.key)

/**
 * Decrypt the [HashedElGamalCiphertext] using the given β = K^R mod p.
 * Returns `null` if the decryption fails, likely from an HMAC verification failure.
 */
fun HashedElGamalCiphertext.decryptWithBeta(beta: ElementModP): ByteArray? =
    decryptAlphaBeta(c0, beta)

/**
 * Decrypt the [HashedElGamalCiphertext] using the given keypair's secret key. Returns
 * `null` if the decryption fails, likely from an HMAC verification failure.
 */
fun HashedElGamalCiphertext.decrypt(keypair: ElGamalKeypair) = decrypt(keypair.secretKey)

/**
 * Attempts to decrypt the [HashedElGamalCiphertext] using the nonce originally used to do the
 * encryption. Returns `null` if the decryption fails, likely from an HMAC verification failure.
 */
fun HashedElGamalCiphertext.decryptWithNonce(keypair: ElGamalKeypair, nonce: ElementModQ): ByteArray? =
    decryptWithNonce(keypair.publicKey, nonce)

/**
 * Attempts to decrypt the [HashedElGamalCiphertext] using the nonce originally used to do the
 * encryption. Returns `null` if the decryption fails, likely from an HMAC verification failure.
 */
fun HashedElGamalCiphertext.decryptWithNonce(publicKey: ElGamalPublicKey, nonce: ElementModQ): ByteArray? {
    // see ByteArray.hashedElGamalEncrypt(), which does the same thing
    val (alpha, beta) = 0.encrypt(publicKey, nonce)
    return decryptAlphaBeta(alpha, beta)
}

private fun HashedElGamalCiphertext.decryptAlphaBeta(alpha: ElementModP, beta: ElementModP): ByteArray? {
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
 *  @param label is a string that identifies the purpose for the derived keying material.
 *  - The [context] is a string containing the information related to the derived keying material.
 *    It may include identities of parties who are deriving and/or using the derived keying
 *    material.
 *  @param length specifies the length of the encrypted message in *bits*, not bytes.
 */
class KDF(val key: UInt256, label: String, context: String, length: Int) {
    // we're going to convert the strings as UTF-8
    private val labelBytes = label.encodeToByteArray()
    private val lengthBytes = length.toByteArray()
    private val contextBytes = context.encodeToByteArray()

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