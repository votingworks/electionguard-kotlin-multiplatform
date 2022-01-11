package electionguard.core

/** Get "secure" random bytes from the native platform */
expect fun randomBytes(length: Int): ByteArray

// Engineering note: It might be tempting to use ktor-utils directly for this, but their
// implementation on Unix is junk. It just calls rand() over and over. Their implementation
// on the JVM is also a bit shaky. Their implementation on JavaScript, however, turns out
// to do exactly the right thing, so we're taking advantage.

// Security bug filed here: https://youtrack.jetbrains.com/issue/KTOR-3656