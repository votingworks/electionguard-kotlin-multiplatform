package electionguard.core

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual fun ByteArray.sha256(): ByteArray {
    val md = MessageDigest.getInstance("SHA-256")
    md.update(this)
    return md.digest()
}

actual fun ByteArray.hmacSha256(key: ByteArray): ByteArray {
    val md = Mac.getInstance("HmacSHA256")
    val secretKey = SecretKeySpec(key, "HmacSHA256")
    md.init(secretKey)
    md.update(this)
    return md.doFinal()
}
