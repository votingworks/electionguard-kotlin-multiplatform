package electionguard.core

/**
 * Fetches the production-strength [GroupContext] with the desired amount of acceleration via
 * precomputation, which can result in significant extra memory usage. This function is "suspending"
 * because, at least in the JavaScript universe, some of the internals require a `Promise` to
 * complete, which we can hide by running in a `suspend` context. For JVM or native projects, just
 * surround this with a call to `runBlocking` or equivalent.
 *
 * @see PowRadixOption for the different memory use vs. performance profiles
 */
expect suspend fun productionGroup(
    acceleration: PowRadixOption = PowRadixOption.LOW_MEMORY_USE
): GroupContext
