package electionguard.core

/** Construct a DLog implementation based on the requested GroupContext. */
expect fun dLoggerOf(context: GroupContext): DLog

/** General-purpose discrete-log engine. */
expect class DLog {
    /**
     * Given an an element x for which there exists an e, such that g^e = x, this will find e, so
     * long as e is "small enough" (typically under 1 billion). This will consume O(e) time, the
     * first time, after which the results are memoized for better future performance.
     */
    fun dLog(input: ElementModP): Int?
}
