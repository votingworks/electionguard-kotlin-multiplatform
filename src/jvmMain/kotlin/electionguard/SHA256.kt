package electionguard

import java.security.MessageDigest

actual fun ByteArray.sha256(): ByteArray {
    val md = MessageDigest.getInstance("SHA-256")
    md.update(this)
    return md.digest()
}
