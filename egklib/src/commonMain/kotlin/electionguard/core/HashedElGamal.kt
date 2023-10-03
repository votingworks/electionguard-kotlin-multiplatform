package electionguard.core

import electionguard.keyceremony.PublicKeys
import kotlin.experimental.xor

/**
 * The ciphertext representation of an arbitrary byte-array, encrypted with an ElGamal public key.
 */
data class HashedElGamalCiphertext(
    val c0: ElementModP,
    val c1: ByteArray,
    val c2: UInt256,
    val numBytes: Int // TODO not sure if this is needed
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

// seems to be specific to contest data, not sure what a general case is.
fun ByteArray.encryptToHashedElGamalCiphertext(
    publicKey: ElGamalPublicKey, // aka K
    extendedBaseHash: UInt256, // aka He
    label: String, // aka Λ
    context: String, // aka Λ
    nonce: UInt256): HashedElGamalCiphertext {

    // D = D_1 ∥ D_2 ∥ · · · ∥ D_bD  ; (spec 2.0, eq 49)
    val messageBlocks: List<UInt256> =
        this.toList()
            .chunked(32) { block ->
                // pad each block of the message to 32 bytes
                val result = ByteArray(32) { 0 }
                block.forEachIndexed { index, byte -> result[index] = byte }
                UInt256(result)
            }

    val group = compatibleContextOrFail(publicKey.key)

    // ElectionGuard spec: (α, β) = (g^ξ mod p, K^ξ mod p); by encrypting a zero, we achieve exactly this
    val (alpha, beta) = 0.encrypt(publicKey, nonce.toElementModQ(group))
    // k = H(HE ; 0x22, K, α, β) ; (spec 2.0, eq 51)
    val kdfKey = hashFunction(extendedBaseHash.bytes, 0x22.toByte(), publicKey.key, alpha, beta)

    // TODO is this eq(52) ??
    // ki = HMAC(k, b(i, 4) ∥ Label ∥ 0x00 ∥ Context ∥ b((bD + 1) · 256, 4)),
    val kdf = KDF(kdfKey, label, context, this.size * 8)

    val k0 = kdf[0]
    val c0 = alpha.byteArray() // (53)
    val encryptedBlocks = messageBlocks.mapIndexed { i, p -> (p xor kdf[i + 1]).bytes }.toTypedArray()
    val c1 = concatByteArrays(*encryptedBlocks) // (54)
    val c2 = (c0 + c1).hmacSha256(k0) // ; eq (55) TODO can we use hmacFunction() ??

    return HashedElGamalCiphertext(alpha, c1, c2, this.size)
}
