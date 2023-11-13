package electionguard.core

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger("HashedElGamalCiphertext")

/**
 * The ciphertext representation of an arbitrary byte-array, encrypted with an ElGamal public key.
 */
data class HashedElGamalCiphertext(
    val c0: ElementModP,
    val c1: ByteArray,
    val c2: UInt256,
    val numBytes: Int // TODO not sure if this is needed
) {

    override fun toString(): String {
        return "HashedElGamalCiphertext(c0=$c0,\n c1=${c1.contentToString()},\n c2=$c2,\n numBytes=$numBytes)"
    }

    // override because of the ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

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

// Hashed ElGamal encryption (3.3.6, p 32)
//
//  Write the contest data field as a concatenation of blocks
//    D = D1 ∥ D2 ∥ · · · ∥ DbD                                                    (49)
// where the Di , 1 ≤ i ≤ bD , consist of 32 bytes each. To encrypt it with the public election key K, a
// pseudo-random nonce ξ is derived from the ballot nonce ξB and the contest label Λ as
//    ξ = H(HE ; 0x20, ξB , indc (Λ), “contest data”)                               (50)
// to compute (α, β) = (g^ξ mod p, K^ξ mod p). A 256-bit secret key is derived as the hash
//    k = H(HE ; 0x22, K, α, β).                                                    (51)
//
// Next, a KDF in counter mode based on HMAC is used to generate a MAC key k0 and encryption
// keys k1 ∥ k2 ∥ · · · ∥ kbD by computing
//   ki = HMAC(k, b(i, 4) ∥ Label ∥ 0x00 ∥ Context ∥ b((bD + 1) · 256, 4))          (52)
// for 0 ≤ i ≤ bD . The label and context byte arrays are Label =
//   b(“data enc keys”, 13) and Context = b(“contest data”, 12) ∥ b(indc (Λ), 4).
// Each ki is a 256-bit key, and b(i, 4) and b((bD + 1) · 256, 4) are byte arrays of the fixed length of 4 bytes
// that encode the integers i and (bD + 1) · 256.
// Therefore, i and (bD + 1) · 256 must be less than 232 , i.e. 0 ≤ i < 232 and 1 ≤ bD + 1 < 224 .
//
// The ciphertext encrypting D is CD = (C0 , C1 , C2 ), where
//   C0 = α = g^ξ mod p                          (53)
//   C1 = D1 ⊕ k1 ∥ D2 ⊕ k2 ∥ · · · ∥ DbD ⊕ kbD  (54)
//   C2 = HMAC(k0 , C0 ∥ C1 )                    (55)
// The component C1 is computed by bitwise XOR (here denoted by ⊕) of each data block Di with
// the corresponding key ki . Component C2 is a message authentication code.
//
// ContestData.encryptContestData(
//    publicKey: ElGamalPublicKey, // aka K
//    extendedBaseHash: UInt256, // aka He
//    contestId: String, // aka Λ
//    contestIndex: Int, // ind_c(Λ)
//    ballotNonce: UInt256): HashedElGamalCiphertext
//
// can be replaced with:
//
//        val contestDataNonce = hashFunction(extendedBaseHash.bytes, 0x20.toByte(), ballotNonce, contestIndex, ContestData.contestDataLabel)
//        val result = testMessage.encryptToHashedElGamal(
//            group,
//            keypair.publicKey,
//            extendedBaseHash,
//            0x22.toByte(),
//            ContestData.label,
//            context = "${ContestData.contestDataLabel}$contestId",
//            contestDataNonce.toElementModQ(group),
//        )

// General form
fun ByteArray.encryptToHashedElGamal(
        group: GroupContext,
        publicKey: ElGamalPublicKey, // aka K
        extendedBaseHash: UInt256, // aka He
        separator: Byte,  // generally choosen for uniqueness
        label: String,  // label for KDF
        context: String,  // contest for KDF
        nonce: ElementModQ = group.randomElementModQ(),
    ): HashedElGamalCiphertext {

    // D = D_1 ∥ D_2 ∥ · · · ∥ D_bD  ;                                                          (eq 49)
    val messageBlocks: List<UInt256> =
        this.toList()
            .chunked(32) { block ->
                // pad each block of the message to 32 bytes
                val result = ByteArray(32) { 0 }
                block.forEachIndexed { index, byte -> result[index] = byte }
                UInt256(result)
            }

    // ElectionGuard spec: (α, β) = (g^ξ mod p, K^ξ mod p); by encrypting a zero, we achieve exactly this
    val (alpha, beta) = 0.encrypt(publicKey, nonce)
    // k = H(HE ; 0x22, K, C0 , β) eq 51: secret key since beta is secret since nonce is secret.
    val kdfKey = hashFunction(extendedBaseHash.bytes, separator, publicKey.key, alpha, beta)

    // ki = HMAC(k, b(i, 4) ∥ Label ∥ 0x00 ∥ Context ∥ b((bD + 1) · 256, 4)) // TODO implementation correct?
    val kdf = KDF(kdfKey, label, context, this.size * 8)
    val k0 = kdf[0]
    val c0 = alpha.byteArray() //                                                                   (eq 53)
    val encryptedBlocks = messageBlocks.mapIndexed { i, p -> (p xor kdf[i + 1]).bytes }.toTypedArray()
    val c1 = concatByteArrays(*encryptedBlocks) //                                                  (eq 54)
    val c2 = (c0 + c1).hmacSha256(k0) // TODO can we use hmacFunction() ??                          (eq 55)

    return HashedElGamalCiphertext(alpha, c1, c2, this.size)
}

// inverse of encryptToHashedElGamal()
fun HashedElGamalCiphertext.decryptToByteArray(
        publicKey: ElGamalPublicKey, // aka K
        extendedBaseHash: UInt256, // aka He
        separator: Byte,  // generally choosen for uniqueness
        label: String,  // label for KDF
        context: String,  // contest for KDF
        alpha: ElementModP, // usually c0
        beta: ElementModP,  // this is after partial decryption, eq 78.
    ): ByteArray? {

    // has to match encryptToHashedElGamal()
    val kdfKey = hashFunction(extendedBaseHash.bytes, separator, publicKey.key, alpha, beta)
    val kdf = KDF(kdfKey, label, context, numBytes * 8) // (86, 87)
    val k0 = kdf[0]
    val expectedHmac = (c0.byteArray() + c1).hmacSha256(k0)
    if (expectedHmac != c2) {
        logger.error { "HashedElGamalCiphertext decryptContestData failure: HMAC doesn't match" }
        return null
    }

    val ciphertextBlocks = c1.toList().chunked(32) { it.toByteArray().toUInt256safe() } //             eq 88
    val plaintextBlocks = ciphertextBlocks.mapIndexed { i, c -> (c xor kdf[i + 1]).bytes }.toTypedArray()
    val plaintext = concatByteArrays(*plaintextBlocks) //                                               eq 89

    return if (plaintext.size == numBytes) {
        plaintext
    } else {
        // Truncate trailing values, which should be zeros.
        // No need to check, because we've already validated the HMAC on the data.
        plaintext.copyOfRange(0, numBytes)
    }
}


