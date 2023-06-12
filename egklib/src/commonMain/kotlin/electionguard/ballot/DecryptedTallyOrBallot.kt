package electionguard.ballot

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModP
import electionguard.core.ChaumPedersenProof
import electionguard.core.HashedElGamalCiphertext

/**
 * The decryption of one encrypted ballot or encrypted tally.
 * The only difference between a decrypted tally and a decrypted ballot is that only a ballot
 * has decryptedContestData.
 *
 * @param id matches the tallyId, or the ballotId if its a ballot decryption.
 * @param contests The contests
 */
data class DecryptedTallyOrBallot(val id: String, val contests: List<Contest>) {

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

    // used for validation; see spec 1.52, section 3.5.4, 4.10.2
    data class DecryptedContestData(
        val contestData: ContestData,
        val encryptedContestData : HashedElGamalCiphertext, // same as EncryptedTally.Selection.ciphertext
        val proof: ChaumPedersenProof,
        var beta: ElementModP,
    )

    /**
     * The decrypted count of one selection of one contest in the election.
     *
     * @param selectionId equals the Manifest.SelectionDescription.selectionId.
     * @param tally     the decrypted vote count.
     * @param value     M = K^tally in the spec. used in verifier
     * @param message   The encrypted vote count = (A, B).
     * @param proof     Proof of correctness
     */
    data class Selection(
        val selectionId: String, // matches SelectionDescription.selectionId
        val tally: Int,         // logK(M)
        val value: ElementModP, // M = K^tally = B/Mbar  // equals 1 when tally = 0; is this needed? 8.B, 11.B.
                                // seems like could compute M and Mbar from B and t, then the challenge in 8 is enough
        val message: ElGamalCiphertext, // (A, B) same as EncryptedTally.Selection.ciphertext
        val proof: ChaumPedersenProof,
    ) {
        init {
            require(selectionId.isNotEmpty())
            require(tally >= 0)
        }
    }
}