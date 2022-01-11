package electionguard.core

import electionguard.core.ElementModP
import electionguard.core.GroupContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

private const val MAX_DLOG: Int = 1_000_000_000

class DLog(val context: GroupContext) {
    // We're taking advantage of Java's ConcurrentHashMap, which allows us to know
    // we can safely attempt reads on the map without needing our global lock, which
    // we only need to use for writes.

    private val dLogMapping: MutableMap<ElementModP, Int> =
        ConcurrentHashMap<ElementModP, Int>()
            .apply {
                this[context.ONE_MOD_P] = 0
            }

    private var dLogMaxElement = context.ONE_MOD_P
    private var dLogMaxExponent = 0

    private val mutex = Mutex()

    fun dLog(input: ElementModP): Int? =
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

                        while (input != dLogMaxElement) {
                            if (dLogMaxExponent++ > MAX_DLOG) {
                                error = true
                                break
                            } else {
                                dLogMaxElement *= context.G_MOD_P
                                dLogMapping[dLogMaxElement] = dLogMaxExponent
                            }
                        }

                        if (error) null else dLogMaxExponent
                    }
                }
            }
        }
}