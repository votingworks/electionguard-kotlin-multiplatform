package electionguard.core

actual class HmacSha256 actual constructor(key : ByteArray) {
    actual fun update(ba : ByteArray) {
    }

    actual fun finish() : UInt256 {
        return UInt256.ZERO
    }
}