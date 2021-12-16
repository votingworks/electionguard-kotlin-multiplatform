package electionguard

import com.soywiz.krypto.*

/**
 * Our own assert function, which isn't available in the Kotlin standard library on JavaScript, even
 * though it's available on JVM and Native. If `condition` is `false`, then an `AssertionError` is
 * thrown with the given message, which defaults to "Assertion failed".
 */
fun assert(condition: Boolean, message: () -> String = { "Assertion failed" }) {
    if (!condition) {
        throw AssertionError(message())
    }
}

/**
 * Throughout our bignum arithmetic, every operation needs to check that its operands are compatible
 * (i.e., that we're not trying to use the test group and the production group interchangeably).
 * This will verify that compatibility and throw an `ArithmeticException` if they're not.
 */
fun GroupContext.assertCompatible(other: GroupContext) {
    if (!this.isCompatible(other)) {
        throw ArithmeticException("incompatible group contexts")
    }
}

/** Computes the SHA256 hash of the given string's UTF-8 representation. */
fun String.sha256(): ByteArray {
    // portable helper function provided by Kotlin
    val stringBytes: ByteArray = this.encodeToByteArray()

    // Krypto provies us with a general-purpose SHA256 hash function
    return stringBytes.sha256().bytes
}

/** Get "secure" random bytes from the native platform. */
fun randomBytes(length: Int): ByteArray {
    // The Krypto library has logic inside of it to deal with the JVM, JS (Node and browser,
    // which are not the same thing), and Native. This saves us a bunch of work.

    val result = ByteArray(length)
    fillRandomBytes(result)
    return result
}