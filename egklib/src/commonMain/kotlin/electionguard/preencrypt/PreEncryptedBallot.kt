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
    val confirmationCode: UInt256, // eq 95
) {
    fun show() {
        println("\nPreEncryptedBallot $ballotId code = $confirmationCode")
        for (pcontest in this.contests) {
            println(" contest ${pcontest.contestId} (votesAllowed=${pcontest.votesAllowed}) = ${pcontest.preencryptionHash.toHex()}")
            for (pselection in pcontest.selections) {
                println("  selection ${pselection.selectionId} (${pselection.sequenceOrder}) = ${pselection.shortCode}")
                pselection.selectionVector.forEach { println("   encryption ${it}") }
            }
        }
        println()
    }
}

data class PreEncryptedContest(
    val contestId: String, // could just pass the manifest contest, in case other info is needed
    val sequenceOrder: Int,
    val votesAllowed: Int, // TODO remove
    val selections: List<PreEncryptedSelection>, // nselections + limit, in sequenceOrder, eq 92,93
    val preencryptionHash: UInt256, // eq 95
)

data class PreEncryptedSelection(
    val selectionId: String, // could just pass the manifest selection, in case other info is needed
    val sequenceOrder: Int,  // matches the Manifest
    val selectionHash: ElementModQ, // allow numerical sorting with ElementModQ, eq 92
    val shortCode: String,
    val selectionVector: List<ElGamalCiphertext>, // nselections, in sequenceOrder, eq 91
    val selectionNonces: List<ElementModQ>, // nselections, in sequenceOrder (optional)
)