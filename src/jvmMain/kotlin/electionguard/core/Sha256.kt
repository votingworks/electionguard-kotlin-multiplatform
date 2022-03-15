package electionguard.core

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual fun ByteArray.sha256(): UInt256 {
    val md = MessageDigest.getInstance("SHA-256")
    md.update(this)
    return UInt256(md.digest())
}

actual fun ByteArray.hmacSha256(key: ByteArray): UInt256 {
    val md = Mac.getInstance("HmacSHA256")
    val secretKey = SecretKeySpec(key, "HmacSHA256")
    md.init(secretKey)
    md.update(this)
    return UInt256(md.doFinal())
}
