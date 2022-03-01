@file:OptIn(ExperimentalCoroutinesApi::class)

package electionguard.core

import kotlin.test.assertFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Kotest requires its properties to be executed as a suspending function. To
 * make this all work, we're using [kotlinx.coroutines.test.runTest] to do it. Note that this
 * internal `runTest` function requires that it be called *at most once per test method*. It's fine
 * to put multiple asserts or `forAll` calls or whatever else inside the `runTest` lambda body.
 */
fun runTest(f: suspend () -> Unit) {
    // another benefit of having this wrapper code: we don't have to have the OptIn thing
    // at the top of every unit test file
    kotlinx.coroutines.test.runTest { f() }
}

/** Verifies that two byte arrays are different. */
fun assertContentNotEquals(a: ByteArray, b: ByteArray, message: String? = null) {
    assertFalse(a.contentEquals(b), message)
}