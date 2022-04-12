package electionguard.core

import hacl.*

actual fun randomBytes(length: Int): ByteArray {
    val result = ByteArray(length)
    result.useNative {
        // This HACL library function is great. It has specialized code for Windows and Unix.
        // If there's a SYS_getrandom system call (available on Linux since 2014), it will
        // use it, and if not, it will read from /dev/urandom.
        Lib_RandomBuffer_System_crypto_random(it, length.toUInt())
    }
    return result
}