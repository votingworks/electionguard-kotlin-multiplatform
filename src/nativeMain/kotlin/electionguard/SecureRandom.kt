package electionguard

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread

/**
 * Cryptographically secure alternative for Kotlin/Native, which doesn't give us something
 * analogous to Java's SecureRandom or JavaScript's Crypto object. This probably won't work
 * on Windows because it tries to read /dev/random. There is a Posix API, getrandom(), which
 * works on Linux and maybe Windows, but not on a Mac.
 */
fun secureRandomBytes(nbytes: Int): ByteArray {
    val result = ByteArray(nbytes)
    val fp = fopen("/dev/random", "rb")
        ?: throw UnsupportedOperationException("this platform doesn't seem to have /dev/random")

    val bytesRead = result.usePinned {
        fread(it.addressOf(0), 1, nbytes.toULong(), fp)
    }

    if (bytesRead != nbytes.toULong())
        throw UnsupportedOperationException("we only got $bytesRead of $nbytes bytes")

    fclose(fp)
    return result
}