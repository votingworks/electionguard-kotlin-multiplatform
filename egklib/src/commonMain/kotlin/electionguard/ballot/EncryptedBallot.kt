package electionguard.ballot

import electionguard.core.*

/**
 * The encrypted representation of a voter's ballot.
 * All contests and selections must be present, so that an inspection of an EncryptedBallot reveals no information.
 */
data class EncryptedBallot(
    override val ballotId: String,
    val ballotStyleId: String,  // matches a Manifest.BallotStyle
    val confirmationCode: UInt256, // tracking code, H(B), eq 59
    val codeBaux: ByteArray, // Baux in eq 59
    override val contests: List<Contest>,
    val timestamp: Long,
    override val state: BallotState,
    val isPreencrypt: Boolean = false,
) : EncryptedBallotIF {

    init {
        require(ballotId.isNotEmpty())
        require(contests.isNotEmpty())
    }

    // override because of codeBaux: ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedBallot) return false

        if (ballotId != other.ballotId) return false
        if (ballotStyleId != other.ballotStyleId) return false
        if (confirmationCode != other.confirmationCode) return false
        if (!codeBaux.contentEquals(other.codeBaux)) return false
        if (contests != other.contests) return false
        if (timestamp != other.timestamp) return false
        if (state != other.state) return false
        return isPreencrypt == other.isPreencrypt
    }

    override fun hashCode(): Int {
        var result = ballotId.hashCode()
        result = 31 * result + ballotStyleId.hashCode()
        result = 31 * result + confirmationCode.hashCode()
        result = 31 * result + codeBaux.contentHashCode()
        result = 31 * result + contests.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + isPreencrypt.hashCode()
        return result
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
        override val contestId: String, // matches ContestDescription.contestIdd
        override val sequenceOrder: Int, // matches ContestDescription.sequenceOrder
        val contestHash: UInt256, // eq 58
        override val selections: List<Selection>,
        val proof: ChaumPedersenRangeProofKnownNonce,
        val contestData: HashedElGamalCiphertext,
        val preEncryption: PreEncryption? = null, // pre-encrypted ballots only
    ) : EncryptedBallotIF.Contest  {

        init {
            require(contestId.isNotEmpty())
            require(selections.isNotEmpty())
        }
    }

    data class Selection(
        override val selectionId: String, // matches SelectionDescription.selectionId
        override val sequenceOrder: Int, // matches SelectionDescription.sequenceOrder
        override val ciphertext: ElGamalCiphertext,
        val proof: ChaumPedersenRangeProofKnownNonce,
    ) : EncryptedBallotIF.Selection  {

        init {
            require(selectionId.isNotEmpty())
        }
    }

    data class PreEncryption(
        val preencryptionHash: UInt256,
        // the selection hashes for every option on the ballot
        val allSelectionHashes: List<UInt256>, // size = nselections + limit, sorted numerically
        // the short codes and selection vectors for all selections on the made by the voter.
        val selectedVectors: List<SelectionVector>, // size = limit, sorted numerically
    ) {
        fun show() {
            println("PreEncryption preencryptionHash = $preencryptionHash")
            println("   allSelectionHashes = $allSelectionHashes")
            for (sv in this.selectedVectors) {
                println("  selection ${sv.shortCode} selectionHash= ${sv.selectionHash}")
                sv.encryptions.forEach { println("   encryption ${it}") }
            }
        }
    }

    data class SelectionVector(
        val selectionHash: UInt256,
        val shortCode: String,
        val encryptions: List<ElGamalCiphertext>, // Ej, size = nselections, in order by sequence_order
    )

}