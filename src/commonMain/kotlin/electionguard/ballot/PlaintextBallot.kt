package electionguard.ballot

/**
 * The plaintext representation of a voter's ballot selections for all the contests in an election.
 * The object_id is a unique Ballot ID created by the external system.
 * This is used both as input, and for the roundtrip: input -&gt; encrypt -&gt; decrypt -&gt; output.
 */
data class PlaintextBallot(
    val ballotId: String,
    val ballotStyleId: String, // matches BallotStyle.object_id
    val contests: List<Contest>
) {

    /**
     * The plaintext representation of a voter's selections for one contest.
     *
     *
     * This can be either a partial or a complete representation of a contest dataset.  Specifically,
     * a partial representation must include at a minimum the "affirmative" selections of a contest.
     * A complete representation of a ballot must include both affirmative and negative selections of
     * the contest, AND the placeholder selections necessary to satisfy the ConstantChaumPedersen proof
     * in the CiphertextBallotContest.
     *
     *
     * Typically partial contests are passed into Electionguard for memory constrained systems,
     * while complete contests are passed into ElectionGuard when running encryption on an existing dataset.
     */
    data class Contest(
        val contestId: String, // matches ContestDescription.object_id
        val sequenceOrder: Int,
        val ballotSelections: List<Selection>,
    );

    /**
     * The plaintext representation of one selection for a particular contest.
     *
     *
     * This can also be designated as `is_placeholder_selection` which has no
     * context to the data specification but is useful for running validity checks internally
     *
     * An `extended_data` field exists to support any arbitrary data to be associated
     * with the selection.  In practice, this field is the cleartext representation
     * of a write-in candidate value.
     * LOOK In the current implementation these write-in are discarded when encrypting.
     */
    data class Selection(
        val selectionId: String, // matches SelectionDescription.object_id
        val sequenceOrder: Int,
        val vote: Int,
        val isPlaceholderSelection: Boolean,
        val extendedData: ExtendedData?
    )

    /** Used to indicate a write-in candidate.  */
    data class ExtendedData(val value: String, val length: Int)
}