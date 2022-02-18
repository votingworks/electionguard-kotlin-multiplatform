package electionguard.ballot

import electionguard.core.ConstantChaumPedersenProofKnownNonce
import electionguard.core.DisjunctiveChaumPedersenProofKnownNonce
import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModQ

data class CiphertextBallot(
    val object_id: String,
    val style_id: String,
    val manifest_hash: ElementModQ,
    val code_seed: ElementModQ,
    val contests: List<Contest>,
    val code: ElementModQ,
    val timestamp: Long,
    val crypto_hash: ElementModQ,
    val nonce: ElementModQ?
) {

    data class Contest(
        val object_id: String,
        val sequence_order: Int,
        val contest_hash: ElementModQ,
        val ballot_selections: List<Selection>,
        val crypto_hash: ElementModQ,
        val encrypted_total: ElGamalCiphertext,
        val nonce: ElementModQ?,
        val proof: ConstantChaumPedersenProofKnownNonce?,
    )

    data class Selection(
        val object_id: String,
        val sequence_order: Int,
        val description_hash: ElementModQ,
        val ciphertext: ElGamalCiphertext,
        val crypto_hash: ElementModQ,
        val is_placeholder_selection: Boolean,
        val nonce: ElementModQ?,
        val proof: DisjunctiveChaumPedersenProofKnownNonce?,
        val extended_data: ElGamalCiphertext?,
    )
}