package electionguard.ballot

// Stripped down manifest for the encryptor
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

    fun contestsForBallotStyle(ballotStyle : String) :  List<Contest>

}