package electionguard

/** Fetches an empty [MutableMap] that will enforce atomicity on its operations. */
expect fun <K, V> emptyConcurrentMutableMap(): MutableMap<K, V>