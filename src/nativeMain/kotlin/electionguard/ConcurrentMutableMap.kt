package electionguard

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Placeholder class until we find something better to ensure atomicity around
 * access to a map. Every single map operation, reading and writing, is wrapped
 * with a lock.
 */
class ConcurrentMutableMap<K, V>() : MutableMap<K, V>, Map<K, V> {
    // TODO: improve this with a single writer lock / multiple reader-lock?

    private var mapImpl: MutableMap<K, V>
    private val mutex: Mutex

    init {
        mapImpl = HashMap()
        mutex = Mutex()
    }

    private fun <T> withLock(f: suspend () -> T): T = runBlocking { mutex.withLock { f() } }

    override fun clear() = withLock { mapImpl = HashMap() }

    override fun put(key: K, value: V) = withLock { mapImpl.put(key, value) }

    override fun putAll(from: Map<out K, V>) = withLock { mapImpl.putAll(from) }

    override fun remove(key: K): V? = withLock { mapImpl.remove(key) }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = withLock { mapImpl.entries }

    override val keys
        get() = withLock { mapImpl.keys }

    override val values
        get() = withLock { mapImpl.values }

    override fun hashCode() = withLock { mapImpl.hashCode() }

    override fun equals(other: Any?) = withLock { mapImpl.equals(other) }

    override fun toString() = withLock { mapImpl.toString() }
    override val size: Int
        get() = withLock { mapImpl.size }

    override fun containsKey(key: K) = withLock { mapImpl.containsKey(key) }

    override fun containsValue(value: V) = withLock { mapImpl.containsValue(value) }

    override fun get(key: K) = withLock { mapImpl.get(key) }

    override fun isEmpty() = withLock { mapImpl.isEmpty() }
}

actual fun <K, V> emptyConcurrentMutableMap(): MutableMap<K, V> = ConcurrentMutableMap()