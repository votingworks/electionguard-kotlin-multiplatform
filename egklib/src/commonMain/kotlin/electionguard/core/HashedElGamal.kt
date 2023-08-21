package electionguard.core

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