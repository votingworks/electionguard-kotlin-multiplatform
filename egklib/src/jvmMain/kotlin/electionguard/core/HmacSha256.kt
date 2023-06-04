package electionguard.core

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual class HmacSha256 actual constructor(key : ByteArray) {
    val md = Mac.getInstance("HmacSHA256")
    init {
        val secretKey = SecretKeySpec(key, "HmacSHA256")
        md.init(secretKey)
    }

    actual fun update(ba : ByteArray) {
        md.update(ba)
    }

    actual fun finish() : UInt256 {
        return UInt256(md.doFinal())
    }
}