package electionguard.ballot

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModQ

/** The encrypted representation of the summed votes for a collection of ballots  */
data class CiphertextTally(
    val objectId: String,
    val contests: Map<String, Contest>
) {

    /**
     * The encrypted selections for a specific contest.
     * The object_id is the Manifest.ContestDescription.object_id.
     */
    data class Contest(
        val objectId: String,
        val sequenceOrder: Int,
        val contestDescriptionHash: ElementModQ,
        val selections: Map<String, Selection>
    )

    /**
     * The homomorphic accumulation of all of the CiphertextBallot.Selection for a specific selection and contest.
     * The object_id is the Manifest.SelectionDescription.object_id.
     */
    data class Selection(
        val objectId: String,
        val sequenceOrder: Int,
        val descriptionHash: ElementModQ,
        val ciphertext: ElGamalCiphertext,
    )
}