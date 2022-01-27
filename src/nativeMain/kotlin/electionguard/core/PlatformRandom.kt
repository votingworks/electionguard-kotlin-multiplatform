package electionguard.core

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.posix.FILE
import platform.posix.fopen
import platform.posix.fread

// TODO: this should work for any Unix platform. If we need to support Windows,
//   there's an entirely different API. See:
//   https://docs.microsoft.com/en-us/windows/win32/api/bcrypt/nf-bcrypt-bcryptgenrandom

/**
 * This class is a wrapper around a file pointer to a device like `/dev/random` or
 * `/dev/urandom` on most Unix systems. It internally uses a mutex to ensure there
 * are no concurrent reads to the file, while still enabling the buffering that
 * Unix `fopen()` and `fread()` might do on our behalf.
 */
private class RandomFileReader(val fp: CPointer<FILE>) {
    val lock = Mutex()

    /** Fetch the requested number of bytes from the random device. */
    suspend fun randomBytes(length: Int): ByteArray {
        val result = ByteArray(length)

        val bytesRead = lock.withLock {
            result.usePinned {
                fread(it.addressOf(0), 1, length.toULong(), fp)
            }
        }

        if (bytesRead != length.toULong())
            throw UnsupportedOperationException("we only got $bytesRead of $length bytes")

        return result
    }
}

private val randomDevice by lazy {
    RandomFileReader(fopen("/dev/random", "rb")
        ?: throw UnsupportedOperationException("this platform doesn't seem to have /dev/random"))
}

actual fun randomBytes(length: Int): ByteArray =
    runBlocking { randomDevice.randomBytes(length) }