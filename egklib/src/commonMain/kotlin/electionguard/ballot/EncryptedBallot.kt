package electionguard.ballot

import electionguard.core.*

/**
 * The encrypted representation of a voter's ballot.
 * All contests and selections must be present, so that an inspection of an EncryptedBallot reveals no information.
 */
data class EncryptedBallot(
    val ballotId: String,
    val ballotStyleId: String,  // matches a Manifest.BallotStyle
    val manifestHash: UInt256,  // matches Manifest.cryptoHash
    val codeSeed: UInt256,
    val code: UInt256,          // confirmation code, aka tracking code
    val contests: List<Contest>,
    val timestamp: Long,
    val cryptoHash: UInt256,
    val state: BallotState,
    val isPreencrypt: Boolean = false,
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
        /** A ballot whose state is unknown to ElectionGuard. */
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
        val preEncryption: PreEncryption? = null, // pre-encrypted ballots only

    )  : CryptoHashableUInt256 {
        init {
            require(contestId.isNotEmpty())
            require(selections.isNotEmpty())
        }
        override fun cryptoHashUInt256() = cryptoHash
    }

    data class PreEncryption(
        val contestHash: UInt256,
        val selectedVectors: List<PreEncryptionVector> = emptyList(), // size = limit, sorted numerically
        // The pre-encryption hashes and associated short codes for every option on the ballot – sorted numerically
        val allHashes: List<PreEncryptionVector> = emptyList(),
    )

    data class PreEncryptionVector(
        val selectionHash: UInt256, // H(Vj)
        val code: String,
        val selectedVector: List<ElGamalCiphertext>, // Vj, size = nselections, in order by sequenceOrder
    )

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