package electionguard.ballot

/**
 * The plaintext representation of a voter's ballot selections for all the contests in an election.
 * The ballotId is a unique Ballot ID created by the external system. This is used both as input,
 * and for the roundtrip: input -> encrypt -> decrypt -> output.
 */
data class PlaintextBallot(
    val ballotId: String, // a unique ballot ID created by the external system
    val ballotStyleId: String, // matches BallotStyle.ballotStyleId
    val contests: List<Contest>,
    val errors: String? = null, // error messages from processing, eg when invalid
) {

    constructor(
        org: PlaintextBallot,
        errors: String
    ) : this(org.ballotId, org.ballotStyleId, org.contests, errors)

    /** The plaintext representation of a voter's selections for one contest. */
    data class Contest(
        val contestId: String, // matches ContestDescription.contestId
        val sequenceOrder: Int,
        val selections: List<Selection>,
    )

    /** The plaintext representation of one selection for a particular contest. */
    data class Selection(
        val selectionId: String, // matches SelectionDescription.selectionId
        val sequenceOrder: Int,
        val vote: Int,
        val isPlaceholderSelection: Boolean,
        val extendedData: ExtendedData?,
    )

    /** Used to indicate a write-in candidate. */
    data class ExtendedData(val value: String, val length: Int)
}

// TODO: right now, we have isPlaceholderSelection as part of the public definition of
//   PlaintextBallot. We're not especially clear on when a voter's plaintext selection,
//   which doesn't have placeholders, gets those placeholders added. This seems to be
//   happening as part of the encryption process, but the data structures above allow
//   for the possibility that it could be done elsewhere. We might want to use the type-
//   system to ensure that we don't ever accidentally get too many placeholders.