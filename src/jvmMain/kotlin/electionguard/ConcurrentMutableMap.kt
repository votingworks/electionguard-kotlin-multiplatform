package electionguard

import java.util.concurrent.ConcurrentHashMap

/** Fetches an empty [MutableMap] that will enforce atomicity on its operations. */
actual fun <K, V> emptyConcurrentMutableMap(): MutableMap<K, V> = ConcurrentHashMap()