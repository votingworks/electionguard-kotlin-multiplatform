package electionguard

// Implementation note for JavaScript: we're in a single-threaded interpreter, so we don't
// have to do anything fancy to make this work.

actual fun <K, V> emptyConcurrentMutableMap(): MutableMap<K, V> = HashMap()
