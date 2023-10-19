package electionguard.core

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

// TODO make this settable. The maximum vote allowed for the tally.
private const val MAX_DLOG: Int = 100_000

actual fun dLoggerOf(base: ElementModP) = DLog(base)

actual class DLog(val base: ElementModP) {

    actual fun base() = base

    // We're taking advantage of Java's ConcurrentHashMap, which allows us to know
    // we can safely attempt reads on the map without needing our global lock, which
    // we only need to use for writes.

    private val dLogMapping: MutableMap<ElementModP, Int> =
        ConcurrentHashMap<ElementModP, Int>()
            .apply {
                this[base.context.ONE_MOD_P] = 0
            }

    private var dLogMaxElement = base.context.ONE_MOD_P
    private var dLogMaxExponent = 0

    private val mutex = Mutex()

    actual fun dLog(input: ElementModP, maxResult: Int): Int? =
        if (input in dLogMapping) {
            dLogMapping[input]
        } else {
            runBlocking {
                mutex.withLock {
                    // We need to check the map again; it might have changed.
                    if (input in dLogMapping) {
                        dLogMapping[input]
                    } else {
                        var error = false
                        val dlogMax = if (maxResult < 0) MAX_DLOG else maxResult

                        while (input != dLogMaxElement) {
                            if (dLogMaxExponent++ > dlogMax) {
                                error = true
                                break
                            } else {
                                dLogMaxElement *= base
                                dLogMapping[dLogMaxElement] = dLogMaxExponent
                            }
                        }

                        if (error) null else dLogMaxExponent
                    }
                }
            }
        }
}