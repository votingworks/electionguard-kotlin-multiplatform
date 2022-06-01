package electionguard.ballot

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModP
import electionguard.decrypt.PartialDecryption

data class PlaintextTally(val tallyId: String, val contests: Map<String, Contest>) {
    /**
     * The plaintext representation of the counts of one contest in the election.
     *
     * @param contestId equals the Manifest.ContestDescription.contestId.
     * @param selections The collection of selections in the contest, keyed by selection.object_id.
     */
    data class Contest(
        val contestId: String, // matches ContestDescription.contestId
        val selections: Map<String, Selection>,
    )

    /**
     * The plaintext representation of the counts of one selection of one contest in the election.
     *
     * @param selectionId equals the Manifest.SelectionDescription.selectionId.
     * @param tally the actual count.
     * @param value g^tally or M in the spec.
     * @param message The encrypted vote count.
     * @param partialDecryptions The Guardians' shares of the decryption of a selection, nguardians of them.
     */
    data class Selection(
        val selectionId: String, // matches SelectionDescription.selectionId
        val tally: Int,
        val value: ElementModP,
        val message: ElGamalCiphertext,
        val partialDecryptions: List<PartialDecryption>,
    )

    fun showTallies() {
        println(" Tally $tallyId")
        contests.values.sortedBy { it.contestId }.forEach { contest ->
            println("  Contest ${contest.contestId}")
            contest.selections.values.sortedBy { it.selectionId }.forEach {
                println("   Selection ${it.selectionId} = ${it.tally}")
            }
        }
    }
}