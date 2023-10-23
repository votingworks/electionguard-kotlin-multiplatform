package electionguard.core

import java.security.MessageDigest

// not used in production, but leaving in for testing internalSha256
fun ByteArray.sha256(): UInt256 {
    val md = MessageDigest.getInstance("SHA-256")
    md.update(this)
    return UInt256(md.digest())
}
