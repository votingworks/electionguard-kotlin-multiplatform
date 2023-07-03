package electionguard.core

// AFAICT, HACL doesnt have HMAC update mode. Since we know that this is the same as
// "concat all ByteArrays and do all at once", we will use that.
actual class HmacSha256 actual constructor(key : ByteArray) {
    var allInput = ByteArray(0)
    val secretKey: ByteArray

    init {
        // Whats the equivalent in HACL land ?? Ill guess its just the regular key
        // secretKey = SecretKeySpec(key, "HmacSHA256")
        secretKey = key
    }

    actual fun update(ba : ByteArray) {
        allInput = allInput + ba
    }

    actual fun finish() : UInt256 {
        return allInput.hmacSha256(secretKey)
    }
}