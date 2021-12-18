package electionguard

expect class DLog {
    /**
     * Computes the discrete log of the input. This function uses memoization so it can reuse prior
     * results. It's also guaranteed to be thread-safe. To avoid potentially huge running times,
     * there's an internal timeout for elements whose discrete log might be larger than a billion.
     * If the limit is hit, `null` is returned and an error is logged.
     */
    fun dLog(input: ElementModP): Int?
}