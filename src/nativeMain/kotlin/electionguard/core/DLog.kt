package electionguard.core

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val MAX_DLOG: Int = 1_000_000_000

actual fun dLoggerOf(base: ElementModP) = DLog(base)

actual class DLog(val b: ElementModP) {
    actual val base: ElementModP
    get() = b

    private val dLogMapping = mutableMapOf(b.context.ONE_MOD_P to 0)

    private var dLogMaxElement = b.context.ONE_MOD_P
    private var dLogMaxExponent = 0

    private val mutex = Mutex()

    actual fun dLog(input: ElementModP): Int? =
        // Unlike the Java version, here we cannot assume that the map allows for
        // reentrant / concurrent reads, so we're using a global lock around it
        // before doing anything at all.

        // Hopefully, this code won't require any optimization. If it does, then
        // we'll have ample opportunity to do it. Example: we could use a functional
        // hash array mapped trie (HAMT), for which multiple pure Kotlin implementations
        // already exist, which are inherently safe for lock-free concurrent reads,
        // although we'd still need a lock to enforce a single-writer policy.

        runBlocking {
            mutex.withLock {
                if (input in dLogMapping) {
                    dLogMapping[input]
                } else {
                    var error = false

                    while (input != dLogMaxElement) {
                        if (dLogMaxExponent++ > MAX_DLOG) {
                            error = true
                            break
                        } else {
                            dLogMaxElement *= b
                            dLogMapping[dLogMaxElement] = dLogMaxExponent
                        }
                    }

                    if (error) null else dLogMaxExponent
                }
            }
        }
}