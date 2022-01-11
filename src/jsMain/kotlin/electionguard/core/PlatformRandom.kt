package electionguard.core

import io.ktor.util.*

// Code borrowed and tweaked from ktor-utils:
// https://github.com/ktorio/ktor/blob/main/ktor-utils/js/src/io/ktor/util/CryptoJs.kt

actual fun randomBytes(length: Int): ByteArray {
    val buffer = ByteArray(length)
    if (PlatformUtils.IS_NODE) {
        _crypto.randomFillSync(buffer)
    } else {
        _crypto.getRandomValues(buffer)
    }
    return buffer
}

private interface Crypto {
    fun randomFillSync(buffer: ByteArray)
    fun getRandomValues(buffer: ByteArray)
}

// Variable is renamed to `_crypto` so it wouldn't clash with existing `crypto` variable.
// JS IR backend doesn't reserve names accessed inside js("") calls
private val _crypto: Crypto by lazy { // lazy because otherwise it's untestable due to evaluation order
    if (PlatformUtils.IS_NODE) {
        js("eval('require')('crypto')")
    } else {
        js("(window.crypto ? window.crypto : window.msCrypto)")
    }
}