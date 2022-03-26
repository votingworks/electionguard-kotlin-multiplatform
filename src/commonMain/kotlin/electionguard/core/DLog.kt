package electionguard.core

/**
 * Construct a DLog implementation for the given `base` element. This might be the group's
 * generator, or it might be any other value in consideration.
 */
expect fun dLoggerOf(base: ElementModP): DLog

/** General-purpose discrete-log engine. */
expect class DLog {
    /** Returns the base used for this particular DLog implementation. */
    val base: ElementModP
        get

    /**
     * Given an an element x for which there exists an e, such that (base)^e = x, this will find e,
     * so long as e is "small enough" (typically under 1 billion). This will consume O(e) time, the
     * first time, after which the results are memoized for better future performance.
     */
    fun dLog(input: ElementModP): Int?
}
