package electionguard

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val MAX_DLOG: Int = 1_000_000_000

class DLog(val context: GroupContext) {
    private val dLogMapping = mutableMapOf(context.ONE_MOD_P to 0)

    private var dLogMaxElement = context.ONE_MOD_P
    private var dLogMaxExponent = 0

    private val mutex = Mutex()

    fun dLog(input: ElementModP): Int? =
        // Unlike the Java version, here we cannot assume that the map allows for
        // reentrant / concurrent reads, so we're using a global lock around it
        // before doing anything at all.

        // Hopefully, this code won't require any optimization. If it does, then
        // we'll have ample opportunity to do it (e.g., by keeping one map instance
        // per thread in ThreadLocal storage).

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
                            dLogMaxElement *= context.G_MOD_P
                            dLogMapping[dLogMaxElement] = dLogMaxExponent
                        }
                    }

                    if (error) null else dLogMaxExponent
                }
            }
        }
}