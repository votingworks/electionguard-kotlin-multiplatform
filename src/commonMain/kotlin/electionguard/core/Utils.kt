package electionguard.core

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

/** Convert an unsigned 64-bit long into a big-endian 8-byte array. */
fun ULong.toByteArray(): ByteArray =
    when {
        this <= UByte.MAX_VALUE -> byteArrayOf((this and 0xffU).toByte())
        this <= UShort.MAX_VALUE ->
            byteArrayOf(((this shr 8) and 0xffU).toByte(), (this and 0xffU).toByte())
        this <= UInt.MAX_VALUE ->
            byteArrayOf(
                ((this shr 24) and 0xffU).toByte(),
                ((this shr 16) and 0xffU).toByte(),
                ((this shr 8) and 0xffU).toByte(),
                (this and 0xffU).toByte()
            )
        else ->
            byteArrayOf(
                ((this shr 56) and 0xffU).toByte(),
                ((this shr 48) and 0xffU).toByte(),
                ((this shr 40) and 0xffU).toByte(),
                ((this shr 32) and 0xffU).toByte(),
                ((this shr 24) and 0xffU).toByte(),
                ((this shr 16) and 0xffU).toByte(),
                ((this shr 8) and 0xffU).toByte(),
                (this and 0xffU).toByte()
            )
    }

/** Convert an unsigned 32-bit int into a big-endian 4-byte array. */
fun UInt.toByteArray(): ByteArray = this.toULong().toByteArray()

/**
 * If there are any null values in the map, the result is null, otherwise the result is the
 * same map, but typed without the nulls.
 */
fun <K, V: Any> Map<K, V?>.noNullValuesOrNull(): Map<K, V>? {
    return if (this.any { it.value == null }) {
        null
    } else {
        @Suppress("UNCHECKED_CAST")
        this as Map<K, V>
    }
}

/**
 * If there are any null values in the list, the result is null, otherwise the result is the
 * same list, but typed without the nulls. Similar to [requireNoNulls], but returns `null`
 * rather than throwing an exception.
 */
fun <T: Any> List<T?>.noNullValuesOrNull(): List<T>? {
    return if (this.any { it == null }) {
        null
    } else {
        @Suppress("UNCHECKED_CAST")
        this as List<T>
    }
}

/**
 * Normally, Kotlin's `Enum.valueOf` or [enumValueOf] method will throw an exception for an
 * invalid input. This method will instead return `null` if the string doesn't map to a valid
 * value of the enum.
 */
inline fun <reified T: Enum<T>> safeEnumValueOf(name: String?): T? {
    if (name == null) {
        return null
    }

    return try {
        enumValueOf<T>(name)
    } catch (e: IllegalArgumentException) {
        null
    }
}
