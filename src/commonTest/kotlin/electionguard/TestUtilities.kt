package electionguard

import io.kotest.assertions.fail

/**
 * Kotest requires its properties to be executed as a suspending function, and doing this isn't
 * quite portable (`runBlocking` on the JVM, isn't available on JS).
 */
expect fun runProperty(f: suspend () -> Unit): Unit

/** Verifies that the lambda body throws the specified exception or error. */
inline fun <reified T : Throwable> assertThrows(message: String = "", f: () -> Unit) {
    try {
        f()
    } catch (ex: Throwable) {
        if (ex is T) {
            return
        }
    }
    fail(message)
}

/** Verifies that the lambda body throws no exceptions. */
inline fun assertDoesNotThrow(message: String = "", f: () -> Unit) {
    try {
        f()
    } catch (ex: Throwable) {
        fail(message)
    }
}
