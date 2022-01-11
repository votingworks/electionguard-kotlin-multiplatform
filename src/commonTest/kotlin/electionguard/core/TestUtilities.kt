@file:OptIn(ExperimentalCoroutinesApi::class)

package electionguard.core

import io.kotest.assertions.fail
import kotlin.test.assertFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Kotest requires its properties to be executed as a suspending function, so we're using a feature
 * from `kotlinx-coroutines-test` to make it happen. Note that the internal `runTest` call requires
 * that it be called *at most once per test method*. It's fine to put multiple asserts or `forAll`
 * calls or whatever else inside the lambda body.
 */
fun runTest(f: suspend () -> Unit) {
    // another benefit of having this wrapper code: we don't have to have the OptIn thing
    // at the top of every unit test file
    kotlinx.coroutines.test.runTest { f() }
}

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

/** Verifies that two byte arrays are different. */
fun assertContentNotEquals(a: ByteArray, b: ByteArray) {
    assertFalse(a.contentEquals(b))
}