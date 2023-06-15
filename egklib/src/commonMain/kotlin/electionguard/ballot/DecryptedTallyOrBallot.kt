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

    data class DecryptedContestData(
        val contestData: ContestData,
        val encryptedContestData : HashedElGamalCiphertext, // same as EncryptedTally.Selection.ciphertext
        val proof: ChaumPedersenProof,
        var beta: ElementModP, // needed to verify 10.2
    )

    /**
     * The decrypted count of one selection of one contest in the election.
     *
     * @param selectionId equals the Manifest.SelectionDescription.selectionId.
     * @param tally     the decrypted vote count.
     * @param value     T = K^tally in the spec. used in verifier
     * @param ciphertext The encrypted vote count
     * @param proof     Proof of correctness that ciphertext encrypts tally
     */
    data class Selection(
        val selectionId: String, // matches SelectionDescription.selectionId
        val tally: Int,         // logK(T)
        val value: ElementModP, // T = K^t mod p; spec 1.9 eq (65)
        val ciphertext: ElGamalCiphertext, // same as EncryptedTally.Selection.ciphertext
        val proof: ChaumPedersenProof,
    ) {
        init {
            require(selectionId.isNotEmpty())
            require(tally >= 0)
        }
    }
}