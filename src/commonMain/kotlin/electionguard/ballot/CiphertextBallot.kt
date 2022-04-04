package electionguard.ballot

import electionguard.core.*

data class CiphertextBallot(
    val ballotId: String,
    val ballotStyleId: String,
    val manifestHash: UInt256, // matches Manifest.cryptoHash
    val codeSeed: UInt256,
    val code: UInt256,
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
        val ciphertextAccumulation: ElGamalCiphertext,
        val cryptoHash: UInt256,
        val proof: ConstantChaumPedersenProofKnownNonce?,
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

fun CiphertextBallot.submit(state: SubmittedBallot.BallotState): SubmittedBallot {
    return SubmittedBallot(
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

fun CiphertextBallot.Contest.submit(): SubmittedBallot.Contest {
    //         val contestId: String, // matches ContestDescription.contestIdd
    //        val sequenceOrder: Int, // matches ContestDescription.sequenceOrderv
    //        val contestHash: UInt256, // matches ContestDescription.cryptoHash
    //        val selections: List<Selection>,
    //        val ciphertextAccumulation: ElGamalCiphertext,
    //        val cryptoHash: UInt256,
    //        val proof: ConstantChaumPedersenProofKnownNonce?,
    return SubmittedBallot.Contest(
        this.contestId,
        this.sequenceOrder,
        this.contestHash,
        this.selections.map { it.submit() },
        this.ciphertextAccumulation,
        this.cryptoHash,
        this.proof,
    )
}

fun CiphertextBallot.Selection.submit(): SubmittedBallot.Selection {
    return SubmittedBallot.Selection(
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