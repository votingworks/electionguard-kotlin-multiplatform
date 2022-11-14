package electionguard.ballot

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModP
import electionguard.core.GenericChaumPedersenProof
import electionguard.core.HashedElGamalCiphertext

/**
 * The decrypted counts of all contests in the election, for one ballot or a collection of ballots.
 *
 * @param id matches the tallyId, or th ballotId if its a ballot decryption.
 * @param contests The contests, keyed by contest.contestId.
 */
data class DecryptedTallyOrBallot(val id: String, val contests: Map<String, Contest>) {

    data class Contest(
        val contestId: String, // matches ContestDescription.contestId
        val selections: Map<String, Selection>, // LOOK why Map?
        val decryptedContestData: DecryptedContestData? = null, // only for ballots
    ) {
        init {
            require(contestId.isNotEmpty())
            require(selections.isNotEmpty())
        }
    }

    // used for validation; see spec 1.52, section 3.5.4, 4.10.2
    data class DecryptedContestData(
        val contestData: ContestData,
        val encryptedContestData : HashedElGamalCiphertext, // same as EncryptedTally.Selection.ciphertext
        val proof: GenericChaumPedersenProof,
        var beta: ElementModP,
    )

    /**
     * The decrypted count of one selection of one contest in the election.
     *
     * @param selectionId equals the Manifest.SelectionDescription.selectionId.
     * @param tally     the decrypted vote count.
     * @param value     g^tally or M in the spec. used in verifier
     * @param message   The encrypted vote count = (A, B).
     * @param proof     Proof of correctness
     */
    data class Selection(
        val selectionId: String, // matches SelectionDescription.selectionId
        val tally: Int, // logK(M)
        val value: ElementModP, // M
        val message: ElGamalCiphertext, // (A, B) same as EncryptedTally.Selection.ciphertext
        val proof: GenericChaumPedersenProof,
    ) {
        init {
            require(selectionId.isNotEmpty())
            require(tally >= 0)
        }
    }

    fun showTally(): String {
        val builder = StringBuilder(5000)
        builder.appendLine(" DecryptedTallyOrBallot $id")
        contests.values.sortedBy { it.contestId }.forEach { contest ->
            builder.appendLine("  Contest ${contest.contestId}")
            contest.selections.values.sortedBy { it.selectionId }.forEach {
                builder.appendLine("   Selection ${it.selectionId} = ${it.tally}")
            }
        }
        return builder.toString()
    }
}