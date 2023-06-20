package electionguard.ballot

import electionguard.core.*

/** Interface used in the crypto routines for easy mocking. */
interface EncryptedBallotIF {

    val ballotId: String
    val contests: List<Contest>
    val state: EncryptedBallot.BallotState

    interface Contest {
        val contestId: String
        val sequenceOrder: Int
        val selections: List<Selection>
    }

    interface Selection {
        val selectionId: String
        val sequenceOrder: Int
        val encryptedVote: ElGamalCiphertext
    }

}