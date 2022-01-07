package electionguard

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
fun String.sha256(): ByteArray = encodeToByteArray().sha256()

/** Get a "secure" random integer from the native platform */
fun randomInt(): Int {
    val bytes = randomBytes(4)
    return (bytes[0].toInt() shl 24) or (bytes[1].toInt() shl 16) or (bytes[2].toInt() shl 8) or
        bytes[3].toInt()
}

/** This should really use a logging library, but we don't have one right now... */
internal fun logWarning(
    @Suppress("UNUSED_PARAMETER")
    f: () -> String
) {
    println(f())
}