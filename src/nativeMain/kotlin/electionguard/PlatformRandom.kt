package electionguard

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread

actual fun randomBytes(length: Int): ByteArray {
    val result = ByteArray(length)

    // TODO: figure out how to do this on Windows -- do we need Windows?

    val fp = fopen("/dev/random", "rb")
        ?: throw UnsupportedOperationException("this platform doesn't seem to have /dev/random")

    val bytesRead = result.usePinned {
        fread(it.addressOf(0), 1, length.toULong(), fp)
    }

    if (bytesRead != length.toULong())
        throw UnsupportedOperationException("we only got $bytesRead of $length bytes")

    fclose(fp)
    return result
}