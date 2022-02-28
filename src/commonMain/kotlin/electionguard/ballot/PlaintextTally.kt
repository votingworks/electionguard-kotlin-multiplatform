package electionguard.ballot

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModP

data class PlaintextTally(val tallyId: String, val contests: Map<String, Contest>) {
    /**
     * The plaintext representation of the counts of one contest in the election. The object_id is
     * the same as the Manifest.ContestDescription.object_id or PlaintextBallotContest object_id.
     *
     * @param selections The collection of selections in the contest, keyed by selection.object_id.
     */
    data class Contest(
        val contestId: String, // matches ContestDescription.contestId
        val selections: Map<String, Selection>,
    );

    /**
     * The plaintext representation of the counts of one selection of one contest in the election.
     * The object_id is the same as the encrypted selection (Ballot.CiphertextSelection) object_id.
     *
     * @param tally the actual count.
     * @param value g^tally or M in the spec.
     * @param message The encrypted vote count.
     * @param shares The Guardians' shares of the decryption of a selection. `M_i` in the spec. Must
     *     be nguardians of them.
     */
    data class Selection(
        val selectionId: String, // matches SelectionDescription.selectionId
        val tally: Int,
        val value: ElementModP,
        val message: ElGamalCiphertext,
        val shares: List<DecryptionShare.CiphertextDecryptionSelection>,
    )
}