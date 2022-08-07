package electionguard.encrypt

import electionguard.ballot.EncryptedBallot
import electionguard.core.*

/** Used only while encrypting. */
data class CiphertextBallot(
    val ballotId: String,
    val ballotStyleId: String,
    val manifestHash: UInt256, // matches Manifest.cryptoHash
    val codeSeed: UInt256,
    val code: UInt256, // tracking code, H_i
    val contests: List<Contest>,
    val timestamp: Long,
    val cryptoHash: UInt256,
    val masterNonce: ElementModQ,
) {
    fun ballotNonce(): UInt256 {
        return hashElements(this.manifestHash, this.ballotId, this.masterNonce)
    }

    data class Contest(
        val contestId: String, // matches ContestDescription.contestIdd
        val sequenceOrder: Int, // matches ContestDescription.sequenceOrder
        val contestHash: UInt256, // matches ContestDescription.cryptoHash
        val selections: List<Selection>,
        val cryptoHash: UInt256,
        val proof: ConstantChaumPedersenProofKnownNonce,
        val contestNonce: ElementModQ,
    ) : CryptoHashableUInt256 {
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
        val selectionNonce: ElementModQ,
    ) : CryptoHashableUInt256 {
        override fun cryptoHashUInt256() = cryptoHash
    }
}

fun CiphertextBallot.submit(state: EncryptedBallot.BallotState): EncryptedBallot {
    return EncryptedBallot(
        this.ballotId,
        this.ballotStyleId,
        this.manifestHash,
        this.codeSeed,
        this.code,
        this.contests.map { it.submit() },
        this.timestamp,
        this.cryptoHash,
        state,
    )
}

fun CiphertextBallot.Contest.submit(): EncryptedBallot.Contest {
    return EncryptedBallot.Contest(
        this.contestId,
        this.sequenceOrder,
        this.contestHash,
        this.selections.map { it.submit() },
        this.cryptoHash,
        this.proof,
    )
}

fun CiphertextBallot.Selection.submit(): EncryptedBallot.Selection {
    return EncryptedBallot.Selection(
        this.selectionId,
        this.sequenceOrder,
        this.selectionHash,
        this.ciphertext,
        this.cryptoHash,
        this.isPlaceholderSelection,
        this.proof,
        this.extendedData,
    )
}