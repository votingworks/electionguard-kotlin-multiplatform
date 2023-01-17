package electionguard.ballot

/**
 * The plaintext representation of a voter's ballot selections as input to the system.
 * The ballotId is a unique Ballot ID created by the external system.
 * Only the contests and selections voted for need be present.
 */
data class PlaintextBallot(
    val ballotId: String,       // a unique ballot ID created by the external system
    val ballotStyleId: String,  // matches a Manifest.BallotStyle
    val contests: List<Contest>,
    val errors: String? = null, // error messages from processing, eg when invalid
) {
    init {
        require(ballotId.isNotEmpty())
    }

    constructor(org: PlaintextBallot, errors: String):
        this(org.ballotId, org.ballotStyleId, org.contests, errors)

    /** The plaintext representation of a voter's selections for one contest. */
    data class Contest(
        val contestId: String, // matches ContestDescription.contestId
        val sequenceOrder: Int, // matches ContestDescription.sequenceOrder
        val selections: List<Selection>,
        val writeIns: List<String> = emptyList(),
    ) {
        init {
            require(contestId.isNotEmpty())
        }
    }

    /** The plaintext representation of one selection for a particular contest. */
    data class Selection(
        val selectionId: String, // matches SelectionDescription.selectionId
        val sequenceOrder: Int, // matches SelectionDescription.sequenceOrder
        val vote: Int,
    )  {
        init {
            require(selectionId.isNotEmpty())
            require(vote >= 0)
        }
    }
}