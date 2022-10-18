package electionguard.ballot

import electionguard.core.*

/**
 * The encrypted representation of a voter's ballot.
 * All contests and selections must be present, so that an inspection of an EncryptedBallot reveals no information.
 */
data class EncryptedBallot(
    val ballotId: String,
    val ballotStyleId: String,
    val manifestHash: UInt256,  // matches Manifest.cryptoHash
    val codeSeed: UInt256,
    val code: UInt256,          // confirmation code, aka tracking code
    val contests: List<Contest>,
    val timestamp: Long,
    val cryptoHash: UInt256,
    val state: BallotState,
) {
    init {
        require(ballotId.isNotEmpty())
        require(contests.isNotEmpty())
    }

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
        val sequenceOrder: Int, // matches ContestDescription.sequenceOrder
        val contestHash: UInt256, // matches ContestDescription.cryptoHash
        val selections: List<Selection>,
        val cryptoHash: UInt256,
        val proof: RangeChaumPedersenProofKnownNonce,
        val contestData: HashedElGamalCiphertext,
    )  : CryptoHashableUInt256 {
        init {
            require(contestId.isNotEmpty())
            require(selections.isNotEmpty())
        }
        override fun cryptoHashUInt256() = cryptoHash
    }

    data class Selection(
        val selectionId: String, // matches SelectionDescription.selectionId
        val sequenceOrder: Int, // matches SelectionDescription.sequenceOrder
        val selectionHash: UInt256, // matches SelectionDescription.cryptoHash
        val ciphertext: ElGamalCiphertext,
        val cryptoHash: UInt256,
        val proof: RangeChaumPedersenProofKnownNonce,
    )  : CryptoHashableUInt256 {
        init {
            require(selectionId.isNotEmpty())
        }
        override fun cryptoHashUInt256() = cryptoHash
    }
}