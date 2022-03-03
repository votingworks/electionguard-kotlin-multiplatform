package electionguard.ballot

import electionguard.core.ConstantChaumPedersenProofKnownNonce
import electionguard.core.DisjunctiveChaumPedersenProofKnownNonce
import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModQ

data class SubmittedBallot(
    val ballotId: String,
    val ballotStyleId: String,
    val manifestHash: ElementModQ,
    val codeSeed: ElementModQ,
    val code: ElementModQ,
    val contests: List<Contest>,
    val timestamp: Long,
    val cryptoHash: ElementModQ,
    val state: BallotState,
) {

    enum class BallotState {
        /** A ballot that has been explicitly cast */
        CAST,
        /** A ballot that has been explicitly spoiled */
        SPOILED,
        /**
         * A ballot whose state is unknown to ElectionGuard and will not be included in any election
         * results.
         */
        UNKNOWN
    }

    data class Contest(
        val contestId: String,
        val sequenceOrder: Int,
        val contestHash: ElementModQ,
        val selections: List<Selection>,
        val ciphertextAccumulation: ElGamalCiphertext,
        val cryptoHash: ElementModQ,
        val proof: ConstantChaumPedersenProofKnownNonce?,
    )

    data class Selection(
        val selectionId: String,
        val sequenceOrder: Int,
        val selectionHash: ElementModQ,
        val ciphertext: ElGamalCiphertext,
        val cryptoHash: ElementModQ,
        val isPlaceholderSelection: Boolean,
        val proof: DisjunctiveChaumPedersenProofKnownNonce?,
        val extendedData: ElGamalCiphertext?,
    )
}