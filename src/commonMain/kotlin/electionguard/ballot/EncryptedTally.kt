package electionguard.ballot

import electionguard.core.ElGamalCiphertext
import electionguard.core.UInt256

/** The encrypted representation of the summed votes for a collection of ballots */
data class EncryptedTally(
    val tallyId: String,
    val contests: List<Contest>
) {
    init {
        require(contests.isNotEmpty())
    }

    /**
     * The encrypted selections for a specific contest. The contestId is the
     * Manifest.ContestDescription.contestId.
     */
    data class Contest(
        val contestId: String,
        val sequenceOrder: Int,
        val contestDescriptionHash: UInt256, // matches ContestDescription.cryptoHash
        val selections: List<Selection>
    ) {
        init {
            require(contestId.isNotEmpty())
            require(selections.isNotEmpty())
        }
    }

    /**
     * The homomorphic accumulation of all of the CiphertextBallot.Selection for a specific
     * selection and contest. The selectionId is the Manifest.SelectionDescription.object_id.
     * @param ciphertext The encrypted vote count = (A, B).
     */
    data class Selection(
        val selectionId: String,
        val sequenceOrder: Int,
        val selectionDescriptionHash: UInt256, // matches SelectionDescription.cryptoHash
        val ciphertext: ElGamalCiphertext,
    ) {
        init {
            require(selectionId.isNotEmpty())
        }
    }
}