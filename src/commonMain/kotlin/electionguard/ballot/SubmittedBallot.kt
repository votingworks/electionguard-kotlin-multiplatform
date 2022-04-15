package electionguard.ballot

import electionguard.core.*

data class SubmittedBallot(
    val ballotId: String,
    val ballotStyleId: String,
    val manifestHash: UInt256, // matches Manifest.cryptoHash
    val codeSeed: UInt256,
    val code: UInt256,
    val contests: List<Contest>,
    val timestamp: Long,
    val cryptoHash: UInt256,
    val state: BallotState,
) {

    enum class BallotState {
        /** A ballot that has been explicitly cast */
        CAST,
        /** A ballot that has been explicitly spoiled */
        SPOILED,
        /** A ballot whose state is unknown to ElectionGuard and will not be included in results. */
        UNKNOWN
    }

    data class Contest(
        val contestId: String, // matches ContestDescription.contestIdd
        val sequenceOrder: Int, // matches ContestDescription.sequenceOrderv
        val contestHash: UInt256, // matches ContestDescription.cryptoHash
        val selections: List<Selection>,
        val ciphertextAccumulation: ElGamalCiphertext,
        val cryptoHash: UInt256,
        val proof: ConstantChaumPedersenProofKnownNonce,
    )  : CryptoHashableUInt256 {
        override fun cryptoHashUInt256() = cryptoHash
    }

    data class Selection(
        val selectionId: String, // matches SelectionDescription.selectionId
        val sequenceOrder: Int, // matches SelectionDescription.sequenceOrder
        val selectionHash: UInt256, // matches SelectionDescription.cryptoHash
        val ciphertext: ElGamalCiphertext,
        val cryptoHash: UInt256,
        val isPlaceholderSelection: Boolean,
        val proof: DisjunctiveChaumPedersenProofKnownNonce,
        val extendedData: HashedElGamalCiphertext?,
    )  : CryptoHashableUInt256 {
        override fun cryptoHashUInt256() = cryptoHash
    }
}