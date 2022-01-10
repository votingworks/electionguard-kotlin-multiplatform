package electionguard

/*
 * Minor modifications of code from JetBrains's ktor:
 * https://github.com/ktorio/ktor/blob/main/ktor-utils/js/src/io/ktor/util/CryptoJs.kt
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import io.ktor.util.*
import org.khronos.webgl.ArrayBuffer
import kotlin.js.Promise

/**
 * Generates a nonce string.
 */
fun jsRandomBytes(nbytes: Int): ByteArray {
    val buffer = ByteArray(nbytes)
    if (PlatformUtils.IS_NODE) {
        _crypto.randomFillSync(buffer)
    } else {
        _crypto.getRandomValues(buffer)
    }
    return buffer
}

// Variable is renamed to `_crypto` so it wouldn't clash with existing `crypto` variable.
// JS IR backend doesn't reserve names accessed inside js("") calls
private val _crypto: Crypto by lazy { // lazy because otherwise it's untestable due to evaluation order
    if (PlatformUtils.IS_NODE) {
        js("eval('require')('crypto')")
    } else {
        js("(window ? (window.crypto ? window.crypto : window.msCrypto) : self.crypto)")
    }
}

private external class Crypto {
    val subtle: SubtleCrypto

    fun getRandomValues(array: ByteArray)

    fun randomFillSync(array: ByteArray)
}

private external class SubtleCrypto {
    fun digest(algoName: String, buffer: ByteArray): Promise<ArrayBuffer>
}