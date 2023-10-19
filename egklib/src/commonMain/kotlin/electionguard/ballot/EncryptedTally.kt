package electionguard.ballot

import electionguard.core.ElGamalCiphertext
import electionguard.core.HashedElGamalCiphertext

/** The encrypted representation of the summed votes for a collection of ballots */
data class EncryptedTally(
    val tallyId: String,
    val contests: List<Contest>,
    val castBallotIds: List<String>,
) {
    init {
        require(contests.isNotEmpty())
    }

    data class Contest(
        val contestId: String,              // matches ContestDescription.contestId
        val sequenceOrder: Int,             // matches ContestDescription.sequenceOrder
        val selections: List<Selection>,
        val contestData: HashedElGamalCiphertext? = null, // only used when decrypting ballots, not tallies
    ) {
        init {
            require(contestId.isNotEmpty())
            require(selections.isNotEmpty())
        }
    }

    data class Selection(
        val selectionId: String,                // matches SelectionDescription.selectionId
        val sequenceOrder: Int,                 // matches SelectionDescription.sequenceOrder
        val encryptedVote: ElGamalCiphertext,   // accumulation over all ballots in the tally
    ) {
        init {
            require(selectionId.isNotEmpty())
        }
    }
}