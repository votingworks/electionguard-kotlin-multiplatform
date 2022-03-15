package electionguard.core

import electionguard.core.Base16.toHex
import kotlin.experimental.xor

/**
 * Superficially similar to an [ElementModQ], but guaranteed to be exactly 32 bytes long. Use with
 * care, because [ByteArray] allows for mutation, and the internal representation is available for
 * external use.
 */
data class UInt256(val bytes: ByteArray) : CryptoHashableString {
    init {
        require (bytes.size == 32) { "UInt256 must have exactly 32 bytes" }
    }

    override fun equals(other: Any?): Boolean = other is UInt256 && other.bytes.contentEquals(bytes)

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    override fun toString(): String {
        return "UInt256(0x${bytes.toHex()})"
    }

    override fun cryptoHashString(): String = bytes.toHex()

    companion object {
        val ZERO = 0U.toUInt256()
        val ONE = 1U.toUInt256()
        val TWO = 2U.toUInt256()
    }
}

/** Computes a new [UInt256] that represents the bitwise xor between two [UInt256] values. */
infix fun UInt256.xor(other: UInt256) =
    UInt256(ByteArray(32) { this.bytes[it] xor other.bytes[it] })

/** Check for whether the UInt256 is all zeros. */
fun UInt256.isZero(): Boolean = UInt256.ZERO == this

/**
 * Given a [ByteArray] representation of a big-endian, unsigned integer, returns a [UInt256] if it
 * fits. Otherwise, throws an [IllegalArgumentException].
 */
fun ByteArray.toUInt256(): UInt256 {
    if (size > 32) {
        throw IllegalArgumentException("Input has $size bytes; UInt256 only supports 32")
    }
    val leftPad = ByteArray(32 - size) { 0 }
    return UInt256(leftPad + this)
}

/**
 * Safely converts a [UInt256] to an [ElementModQ], wrapping values outside the range back to the
 * beginning by computing "mod q".
 */
fun UInt256.toElementModQ(context: GroupContext): ElementModQ =
    context.safeBinaryToElementModQ(bytes)

fun ElementModQ.toUInt256(): UInt256 = UInt256(this.byteArray())
fun ULong.toUInt256(): UInt256 = this.toByteArray().toUInt256()
fun UInt.toUInt256(): UInt256 = this.toULong().toByteArray().toUInt256()
fun UShort.toUInt256(): UInt256 = this.toULong().toByteArray().toUInt256()