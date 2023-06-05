package electionguard.ballot

import electionguard.core.*

/**
 * The encrypted representation of a voter's ballot.
 * All contests and selections must be present, so that an inspection of an EncryptedBallot reveals no information.
 */
data class EncryptedBallot(
    val ballotId: String,
    val ballotStyleId: String,  // matches a Manifest.BallotStyle
    val confirmationCode: UInt256, // tracking code, H(B), eq 59
    val codeBaux: ByteArray, // Baux in eq 59
    val contests: List<Contest>,
    val timestamp: Long,
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
        val contestHash: UInt256, // eq 58
        val selections: List<Selection>,
        val proof: RangeChaumPedersenProofKnownNonce,
        val contestData: HashedElGamalCiphertext,
        val preEncryption: PreEncryption? = null, // pre-encrypted ballots only

    )  {
        init {
            require(contestId.isNotEmpty())
            require(selections.isNotEmpty())
        }
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
        val ciphertext: ElGamalCiphertext,
        val proof: RangeChaumPedersenProofKnownNonce,
    )   {
        init {
            require(selectionId.isNotEmpty())
        }
    }
}