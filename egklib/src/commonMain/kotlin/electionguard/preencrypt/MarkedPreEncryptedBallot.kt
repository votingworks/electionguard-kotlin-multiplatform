package electionguard.preencrypt

/** An internal record representing a cast PreEncryptedBallot, containing the voter choices. */
internal data class MarkedPreEncryptedBallot(
    val ballotId: String,
    val ballotStyleId: String,
    val contests: List<MarkedPreEncryptedContest>,
)  {
    fun show() {
        println("MarkedPreEncryptedBallot ${ballotId} style= ${ballotStyleId}")
        for (contest in this.contests) {
            println(" contest ${contest.contestId}")
            contest.selectedCodes.forEachIndexed { idx, selectionCode ->
                println("  selection ${selectionCode} ${contest.selectedIds[idx]} ")
            }
        }
        println()
    }
}

internal data class MarkedPreEncryptedContest(
    val contestId: String,
    val selectedCodes: List<String>, // voter selected "short codes". may be empty.
    val selectedIds: List<String> = emptyList(), // debugging
)
