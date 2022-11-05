package electionguard.preencrypt

/** An internal record representing a cast PreEncryptedBallot, containing the voter choices. */
internal data class MarkedPreEncryptedBallot(
    val ballotId: String,
    val ballotStyleId: String,
    val contests: List<MarkedPreEncryptedContest>,
)  {
    fun show() {
        println("MarkedPreEncryptedBallot ${ballotId} = ${ballotStyleId}")
        for (contest in this.contests) {
            println(" contest ${contest.contestId}")
            for (selectionCode in contest.selectedCodes) {
                println("  selection ${selectionCode}")
            }
        }
    }
}

internal data class MarkedPreEncryptedContest(
    val contestId: String,
    val selectedCodes: List<String>, // voter selected "short codes"
)
