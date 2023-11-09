package electionguard.ballot

/** Interface used in the crypto routines for easy mocking. */
interface ManifestIF {
    val contests: List<Contest>

    interface Contest {
        val contestId: String
        val sequenceOrder: Int
        val selections: List<Selection>
    }

    interface Selection {
        val selectionId: String
        val sequenceOrder: Int
    }

    /** get the list of valid contests for the given ballotStyle */
    fun contestsForBallotStyle(ballotStyle : String): List<Contest>?

    fun findContest(contestId: String): Contest?

    /** get the contest selection limit (aka votesAllowed) for the given contest id */
    fun contestLimit(contestId : String): Int

    /** get the option selection limit for the given contest id */
    fun optionLimit(contestId : String): Int

}