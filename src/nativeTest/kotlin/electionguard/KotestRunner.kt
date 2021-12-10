package electionguard

import io.kotest.common.runBlocking

actual fun runProperty(f: suspend () -> Unit): Unit {
    runBlocking(f)
}
