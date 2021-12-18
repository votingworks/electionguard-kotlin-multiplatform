package electionguard

/** Get "secure" random bytes from the native platform */
expect fun randomBytes(length: Int): ByteArray