package electionguard.ballot

import electionguard.core.*

/**
 * The encrypted representation of a voter's ballot.
 * All contests and selections must be present, so that an inspection of an EncryptedBallot reveals no information.
 */
data class EncryptedBallot(
        override val ballotId: String,
        val ballotStyleId: String,  // matches a Manifest.BallotStyle
        val encryptingDevice: String,
        val timestamp: Long,
        val codeBaux: ByteArray, // Baux in spec 2.0.0 eq 58
        val confirmationCode: UInt256, // tracking code = H(B) eq 58
        override val electionId : UInt256,
        override val contests: List<Contest>,
        override val state: BallotState,
        val encryptedSn: ElGamalCiphertext?,
        val isPreencrypt: Boolean = false,
    ) : EncryptedBallotIF {

    init {
        require(ballotId.isNotEmpty())
        require(contests.isNotEmpty())
    }

    // override because of codeBaux: ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedBallot

        if (ballotId != other.ballotId) return false
        if (ballotStyleId != other.ballotStyleId) return false
        if (encryptingDevice != other.encryptingDevice) return false
        if (timestamp != other.timestamp) return false
        if (!codeBaux.contentEquals(other.codeBaux)) return false
        if (confirmationCode != other.confirmationCode) return false
        if (electionId != other.electionId) return false
        if (contests != other.contests) return false
        if (state != other.state) return false
        if (encryptedSn != other.encryptedSn) return false
        if (isPreencrypt != other.isPreencrypt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ballotId.hashCode()
        result = 31 * result + ballotStyleId.hashCode()
        result = 31 * result + encryptingDevice.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + codeBaux.contentHashCode()
        result = 31 * result + confirmationCode.hashCode()
        result = 31 * result + electionId.hashCode()
        result = 31 * result + contests.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + (encryptedSn?.hashCode() ?: 0)
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
        val votesAllowed: Int, // matches ContestDescription.votesAllowed TODO remove
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
        override val encryptedVote: ElGamalCiphertext,
        val proof: ChaumPedersenRangeProofKnownNonce,
    ) : EncryptedBallotIF.Selection  {

        init {
            require(selectionId.isNotEmpty())
        }
    }

    data class PreEncryption(
        val preencryptionHash: UInt256, // (eq 95)
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

    // Ψi,m = ⟨E1 , E2 , . . . , Em ⟩  (eq 92)
    data class SelectionVector(
        val selectionHash: UInt256, // ψi (eq 93)
        val shortCode: String,
        val encryptions: List<ElGamalCiphertext>, // Ej, size = nselections, in order by sequence_order
    )

}