package electionguard.core

actual fun ByteArray.sha256(): ByteArray = internalSha256(this)

actual fun ByteArray.hmacSha256(key: ByteArray): ByteArray = internalHmacSha256(key, this)