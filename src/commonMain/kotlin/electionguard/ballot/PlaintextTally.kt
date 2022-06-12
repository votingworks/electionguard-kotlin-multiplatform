package electionguard.ballot

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModP
import electionguard.decrypt.PartialDecryption

/**
 * The decrypted counts of all contests in the election.
 *
 * @param tallyId the name of the tally. Matches ballotId when its a spoiled ballot decryption.
 * @param contests The contests, keyed by contest.contestId.
 */
data class PlaintextTally(val tallyId: String, val contests: Map<String, Contest>) {
    /**
     * The decrypted counts of one contest in the election.
     *
     * @param contestId equals the Manifest.ContestDescription.contestId.
     * @param selections The collection of selections in the contest, keyed by selection.selectionId.
     */
    data class Contest(
        val contestId: String, // matches ContestDescription.contestId
        val selections: Map<String, Selection>,
    ) {
        init {
            require(contestId.isNotEmpty())
            require(selections.isNotEmpty())
        }
    }

    /**
     * The decrypted count of one selection of one contest in the election.
     *
     * @param selectionId equals the Manifest.SelectionDescription.selectionId.
     * @param tally the decrypted vote count.
     * @param value g^tally or M in the spec.
     * @param message The encrypted vote count = (A, B).
     * @param partialDecryptions The Guardians' shares of the decryption of a selection, nguardians of them.
     */
    data class Selection(
        val selectionId: String, // matches SelectionDescription.selectionId
        val tally: Int,
        val value: ElementModP,
        val message: ElGamalCiphertext, // same as EncryptedTally.Selection.ciphertext
        val partialDecryptions: List<PartialDecryption>, // one for each guardian
    ) {
        init {
            require(selectionId.isNotEmpty())
            require(tally >= 0)
            require(partialDecryptions.isNotEmpty())
        }
    }

    fun showTally(): String {
        val builder = StringBuilder(5000)
        builder.appendLine(" Tally $tallyId")
        contests.values.sortedBy { it.contestId }.forEach { contest ->
            builder.appendLine("  Contest ${contest.contestId}")
            contest.selections.values.sortedBy { it.selectionId }.forEach {
                builder.appendLine("   Selection ${it.selectionId} = ${it.tally}")
            }
        }
        return builder.toString()
    }
}