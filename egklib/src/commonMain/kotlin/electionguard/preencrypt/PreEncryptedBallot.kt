package electionguard.preencrypt

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModQ
import electionguard.core.UInt256
import electionguard.core.toUInt256

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
            println(" contest ${contest.contestId} (${contest.votesAllowed}) = ${contest.contestHash.toHex()}")
            for (selection in contest.selectionsSorted) {
                println("  selection ${selection.selectionId} = ${selection.shortCode}")
            }
        }
    }
}

data class PreEncryptedContest(
    val contestId: String, // could just pass the manifest contest, in case other info is needed
    val sequenceOrder: Int,
    val votesAllowed: Int,
    val selectionsSorted: List<PreEncryptedSelection>, // nselections + limit, numerically sorted
    val contestHash: UInt256, // eq 95
) {
    fun selectionHashes() = selectionsSorted.map { it.selectionHash.toUInt256() }
}

data class PreEncryptedSelection(
    val selectionId: String, // could just pass the manifest selection, in case other info is needed
    val sequenceOrder: Int,
    val selectionHash: ElementModQ, // allow numerical sorting with ElementModQ, eq 93
    val shortCode: String,
    val selectionVector: List<ElGamalCiphertext>,
)