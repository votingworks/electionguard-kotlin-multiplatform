package electionguard.core

expect class HmacSha256(key : ByteArray) {
    fun update(ba : ByteArray)
    fun finish() : UInt256
}