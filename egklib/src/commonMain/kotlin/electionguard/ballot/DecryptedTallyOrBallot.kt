package electionguard.ballot

import electionguard.core.*

/**
 * The decryption of one encrypted ballot or encrypted tally.
 * The only difference between a decrypted tally and a decrypted ballot is that only a ballot
 * has DecryptedContestData.
 *
 * @param id matches the tallyId, or the ballotId if its a ballot decryption.
 * @param contests The contests
 */
data class DecryptedTallyOrBallot(
    val id: String,
    val contests: List<Contest>,
    val electionId : UInt256,
) {

    data class Contest(
        val contestId: String, // matches ContestDescription.contestId
        val selections: List<Selection>,
        val decryptedContestData: DecryptedContestData? = null, // only for ballots
    ) {
        init {
            require(contestId.isNotEmpty())
            require(selections.isNotEmpty())
        }
    }

    data class DecryptedContestData(
        val contestData: ContestData,
        val encryptedContestData : HashedElGamalCiphertext, // same as EncryptedTally.Contest.contestData
        val proof: ChaumPedersenProof,
        var beta: ElementModP, // needed to verify 10.2
    )

    /**
     * The decrypted count of one selection of one contest in the election.
     *
     * @param selectionId equals the Manifest.SelectionDescription.selectionId.
     * @param tally     the decrypted vote count.
     * @param bOverM    T = (B / M) mod p. (spec 2.0, eq 64), needed for verification 9.A.
     * @param encryptedVote The encrypted vote count
     * @param proof     Proof of correctness that ciphertext encrypts tally
     */
    data class Selection(
        val selectionId: String, // matches SelectionDescription.selectionId
        val tally: Int,         // logK(T), ie the decrypted vote
        val bOverM: ElementModP, // T = (B / M) mod p. (spec 2.0, eq 64)
        val encryptedVote: ElGamalCiphertext, // same as EncryptedTally.Selection.encryptedVote
        val proof: ChaumPedersenProof, // proof that M = A^s mod p
    ) {
        init {
            require(selectionId.isNotEmpty())
            require(tally >= 0)
        }
    }

    fun show() : String = buildString {
        appendLine("Ballot ${id}")
        contests.forEach { contest ->
            appendLine("  Contest ${contest.contestId}")
            contest.selections.forEach {
                appendLine("    Selection ${it.selectionId} ${it.tally} ")
            }
        }
    }
}