package electionguard.ballot

import electionguard.core.ConstantChaumPedersenProofKnownNonce
import electionguard.core.DisjunctiveChaumPedersenProofKnownNonce
import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModQ

data class SubmittedBallot(
    val objectId: String,
    val ballotStyleId: String,
    val manifestHash: ElementModQ,
    val trackingHash: ElementModQ,
    val previousTrackingHash: ElementModQ,
    val contests: List<Contest>,
    val timestamp: Long,
    val cryptoHash: ElementModQ,
    val nonce: ElementModQ?,
    val state: BallotState,
) {

    enum class BallotState {
        /** A ballot that has been explicitly cast  */
        CAST,
        /** A ballot that has been explicitly spoiled  */
        SPOILED,
        /** A ballot whose state is unknown to ElectionGuard and will not be included in any election results.  */
        UNKNOWN
    }

    data class Contest(
        val contestId: String,
        val sequenceOrder: Int,
        val contestHash: ElementModQ,
        val ballotSelections: List<Selection>,
        val cryptoHash: ElementModQ,
        val encryptedTotal: ElGamalCiphertext,
        val nonce: ElementModQ?,
        val proof: ConstantChaumPedersenProofKnownNonce?,
    )

    data class Selection(
        val selectionId: String,
        val sequenceOrder: Int,
        val descriptionHash: ElementModQ,
        val ciphertext: ElGamalCiphertext,
        val cryptoHash: ElementModQ,
        val isPlaceholderSelection: Boolean,
        val nonce: ElementModQ?,
        val proof: DisjunctiveChaumPedersenProofKnownNonce?,
        val extendedData: ElGamalCiphertext?,
    )
}