package electionguard

// Only "expect" stubs should go in this file. Other shared functionality should go in
// other classes. We're trying to avoid confusion where Kotlin generates two classes
// for the JVM, both named GroupKt, which then doesn't work.

/**
 * Fetches the production-strength [GroupContext] with the desired amount of acceleration via
 * precomputation, which can result in significant extra memory usage. This function is "suspending"
 * because, at least in the JavaScript universe, some of the internals require a promise to
 * complete.
 *
 * @see PowRadixOption for the different memory use vs. performance profiles
 */
expect suspend fun productionGroup(
    acceleration: PowRadixOption = PowRadixOption.LOW_MEMORY_USE
): GroupContext
