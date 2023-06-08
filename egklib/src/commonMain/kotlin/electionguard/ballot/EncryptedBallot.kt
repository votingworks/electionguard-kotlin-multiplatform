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

    data class PreEncryption(
        val contestHash: UInt256,
        // the selection hashes for every option on the ballot
        val allSelectionHashes: List<UInt256>, // size = nselections + limit, sorted numerically
        // the short codes and selection vectors for all selections on the made by the voter.
        val selectedVectors: List<SelectionVector>, // size = limit, sorted numerically
    )

    data class SelectionVector(
        val selectionHash: UInt256,
        val shortCode: String,
        val encryptions: List<ElGamalCiphertext>,
    )

}