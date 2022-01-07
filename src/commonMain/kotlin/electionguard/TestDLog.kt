package electionguard
private const val MAX_DLOG: Int = intTestQ

class TestDLog(val context: GroupContext) {
    // Implementation note for dealing with test-sized values: we're avoiding
    // concurrency issues here by simply precomputing the entire table up front,
    // since it's pretty small. This will yield constant-time dLog computation,
    // and will obviously not be scalable beyond the test group.

    private val dLogMapping: Map<ElementModP, Int> by
        lazy {
            // Keeping this lazy so we don't try to compute anything here while
            // we're still initializing the GroupContext. This will be realized
            // on the first usage.
            (0..intTestQ - 1).map { i -> context.gPowP(i.toElementModQ(context)) to i }.toMap()
        }

    fun dLog(input: ElementModP): Int? = dLogMapping[input]
}