package electionguard

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val MAX_DLOG: Int = 1_000_000_000

actual class DLog(val context: GroupContext) {
    private val dLogMapping: MutableMap<ElementModP, Int> =
        emptyConcurrentMutableMap<ElementModP, Int>().apply { this[context.ONE_MOD_P] = 0 }

    private var dLogMaxElement = context.ONE_MOD_P
    private var dLogMaxExponent = 0

    private val mutex = Mutex()

    actual fun dLog(input: ElementModP): Int? =
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