package electionguard

import io.kotest.common.runPromise

actual fun runProperty(f: suspend () -> Unit): Unit {
    runPromise(f)
}
