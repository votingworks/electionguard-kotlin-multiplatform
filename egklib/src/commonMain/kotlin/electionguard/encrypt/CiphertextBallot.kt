package electionguard.encrypt

import electionguard.ballot.EncryptedBallot
import electionguard.core.*

/** Intermediate stage while encrypting. Does not have the extra Pre-encrytion info, nor the state. */
data class CiphertextBallot(
    val ballotId: String,
    val ballotStyleId: String,
    val encryptingDevice: String,
    val timestamp: Long,
    val codeBaux: ByteArray, // Baux in spec 2.0.0, eq 58
    val confirmationCode: UInt256, // tracking code, H(B), eq 58
    val contests: List<Contest>,
    val ballotNonce: UInt256,
    val isPreEncrypt: Boolean = false,
) {
    data class Contest(
        val contestId: String, // matches ContestDescription.contestIdd
        val sequenceOrder: Int, // matches ContestDescription.sequenceOrder
        val votesAllowed: Int,
        val contestHash: UInt256, // eq 57
        val selections: List<Selection>,
        val proof: ChaumPedersenRangeProofKnownNonce,
        val contestData: HashedElGamalCiphertext,
    )

    data class Selection(
        val selectionId: String, // matches SelectionDescription.selectionId
        val sequenceOrder: Int, // matches SelectionDescription.sequenceOrder
        val ciphertext: ElGamalCiphertext, //  the encrypted vote
        val proof: ChaumPedersenRangeProofKnownNonce,
        val selectionNonce: ElementModQ,
    )
}

fun CiphertextBallot.cast(): EncryptedBallot {
    return this.submit(EncryptedBallot.BallotState.CAST)
}

fun CiphertextBallot.spoil(): EncryptedBallot {
    return this.submit(EncryptedBallot.BallotState.SPOILED)
}

fun CiphertextBallot.submit(state: EncryptedBallot.BallotState): EncryptedBallot {
    return EncryptedBallot(
        this.ballotId,
        this.ballotStyleId,
        this.encryptingDevice,
        this.timestamp,
        this.codeBaux,
        this.confirmationCode,
        this.contests.map { it.submit() },
        state,
        this.isPreEncrypt,
    )
}

fun CiphertextBallot.Contest.submit(): EncryptedBallot.Contest {
    return EncryptedBallot.Contest(
        this.contestId,
        this.sequenceOrder,
        this.votesAllowed,
        this.contestHash,
        this.selections.map { it.submit() },
        this.proof,
        this.contestData,
    )
}

fun CiphertextBallot.Selection.submit(): EncryptedBallot.Selection {
    return EncryptedBallot.Selection(
        this.selectionId,
        this.sequenceOrder,
        this.ciphertext,
        this.proof,
    )
}