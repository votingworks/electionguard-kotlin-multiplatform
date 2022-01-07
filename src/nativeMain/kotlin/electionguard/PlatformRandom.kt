package electionguard

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.fopen
import platform.posix.fread

private val randomDeviceFp by lazy {
    // we won't actually open the file until the first call to randomBytes

    fopen("/dev/random", "rb")
        ?: throw UnsupportedOperationException("this platform doesn't seem to have /dev/random")
}

actual fun randomBytes(length: Int): ByteArray {
    val result = ByteArray(length)

    // TODO: figure out how to do this on Windows -- do we need Windows?

    val bytesRead = result.usePinned {
        fread(it.addressOf(0), 1, length.toULong(), randomDeviceFp)
    }

    if (bytesRead != length.toULong())
        throw UnsupportedOperationException("we only got $bytesRead of $length bytes")

    // Note: we're explicitly not closing the device, so we can get more bytes later
    // on without having to reopen. This, plus whatever buffering is done within
    // boring Unix fopen/fread, should hopefully give us decent performance.

    return result
}