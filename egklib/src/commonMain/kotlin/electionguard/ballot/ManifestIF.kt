package electionguard.ballot

/** Interface used in the crypto routines for easy mocking. */
interface ManifestIF {
    val contests: List<Contest>

    interface Contest {
        val contestId: String
        val sequenceOrder: Int
        val votesAllowed: Int
        val selections: List<Selection>
    }

    interface Selection {
        val selectionId: String
        val sequenceOrder: Int
    }

    /** get the list of valid contests for the given ballotStyle */
    fun contestsForBallotStyle(ballotStyle : String): List<Contest>

    /** get the contest limit (aka votesAllowed) for the given contest id */
    fun contestLimit(contestId : String): Int

}