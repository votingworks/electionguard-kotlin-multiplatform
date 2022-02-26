package electionguard.ballot

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModQ

/** The encrypted representation of the summed votes for a collection of ballots  */
data class CiphertextTally(
    val tallyId: String,
    val contests: Map<String, Contest> // map<contestId, contest>
) {

    /**
     * The encrypted selections for a specific contest.
     * The contestId is the Manifest.ContestDescription.contestId.
     */
    data class Contest(
        val contestId: String,
        val sequenceOrder: Int,
        val contestDescriptionHash: ElementModQ,
        val selections: Map<String, Selection> // map<selectionId, selection>
    )

    /**
     * The homomorphic accumulation of all of the CiphertextBallot.Selection for a specific selection and contest.
     * The object_id is the Manifest.SelectionDescription.object_id.
     */
    data class Selection(
        val selectionId: String,
        val sequenceOrder: Int,
        val selectionDescriptionHash: ElementModQ,
        val ciphertext: ElGamalCiphertext,
    )
}