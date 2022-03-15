package electionguard.core

/** Computes the SHA256 hash of the given byte array */
expect fun ByteArray.sha256(): UInt256

/** Computes the HMAC-SHA256 of the given byte array using the given key */
expect fun ByteArray.hmacSha256(key: ByteArray): UInt256