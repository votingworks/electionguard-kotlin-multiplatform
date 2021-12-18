package electionguard

import java.security.SecureRandom

private val rng = SecureRandom.getInstanceStrong()

actual fun randomBytes(length: Int): ByteArray {
    val bytes = ByteArray(length)
    rng.nextBytes(bytes)
    return bytes
}