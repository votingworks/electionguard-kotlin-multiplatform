package electionguard

actual fun randomBytes(length: Int): ByteArray = jsRandomBytes(length)