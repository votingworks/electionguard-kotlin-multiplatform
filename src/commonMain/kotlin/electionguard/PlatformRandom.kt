package electionguard

/** Get "secure" random bytes from the native platform */
expect fun randomBytes(length: Int): ByteArray

// Engineering note: It might be tempting to use ktor-utils directly for this, but their
// implementation on Unix is junk. It just calls rand() over and over. So we have to do
// our own thing.