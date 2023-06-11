package electionguard.preencrypt

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModQ
import electionguard.core.UInt256

/**
 * The result of PreEncryptor.preencrypt(), for use by the "Encrypting Tool" to make a pre-encrypted ballot.
 */
data class PreEncryptedBallot(
    val ballotId: String,
    val ballotStyleId: String,
    val primaryNonce: UInt256,
    val contests: List<PreEncryptedContest>,
    val confirmationCode: UInt256,
) {
    fun show() {
        println("\nPreEncryptedBallot $ballotId code = $confirmationCode")
        for (contest in this.contests) {
            println(" contest ${contest.contestId} (votesAllowed=${contest.votesAllowed}) = ${contest.preencryptionHash.toHex()}")
            for (selection in contest.selections) {
                println("  selection ${selection.selectionId} (${selection.sequenceOrder}) = ${selection.shortCode}")
                selection.selectionVector.forEach { println("   encryption ${it}") }
            }
        }
        println()
    }
}

data class PreEncryptedContest(
    val contestId: String, // could just pass the manifest contest, in case other info is needed
    val sequenceOrder: Int,
    val votesAllowed: Int,
    val selections: List<PreEncryptedSelection>, // nselections + limit, in sequenceOrder
    val preencryptionHash: UInt256, // eq 95
)

data class PreEncryptedSelection(
    val selectionId: String, // could just pass the manifest selection, in case other info is needed
    val sequenceOrder: Int,  // matches the Manifest
    val selectionHash: ElementModQ, // allow numerical sorting with ElementModQ, eq 93
    val shortCode: String,
    val selectionVector: List<ElGamalCiphertext>, // in sequenceOrder
    // LOOK why dont we just store the damn nonces ??
)