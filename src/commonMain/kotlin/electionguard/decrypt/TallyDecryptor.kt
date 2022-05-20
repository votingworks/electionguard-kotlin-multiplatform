package electionguard.decrypt

import electionguard.ballot.CiphertextTally
import electionguard.ballot.PlaintextTally
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.GroupContext

// Set this to -1 except when testing
private const val maxDlog: Int = 1000

/** After gathering the shares for all guardians (partial or compensated), we can decrypt */
class TallyDecryptor(val group: GroupContext, val publicKey: ElGamalPublicKey, private val nguardians: Int) {

    /** Shares are in a Map keyed by "${contestId}#@${selectionId}" */
    fun decryptTally(tally: CiphertextTally, shares: Map<String, List<PartialDecryption>>): PlaintextTally {
        val contests: MutableMap<String, PlaintextTally.Contest> = HashMap()
        for (tallyContest in tally.contests) {
            val plaintextTallyContest = decryptContestWithDecryptionShares(tallyContest, shares)
            contests[tallyContest.contestId] = plaintextTallyContest
        }
        return PlaintextTally(tally.tallyId, contests)
    }

    private fun decryptContestWithDecryptionShares(
        contest: CiphertextTally.Contest,
        shares: Map<String, List<PartialDecryption>>,
    ): PlaintextTally.Contest {
        val selections: MutableMap<String, PlaintextTally.Selection> = HashMap()
        for (tallySelection in contest.selections) {
            val id = "${contest.contestId}#@${tallySelection.selectionId}"
            val sshares = shares[id] ?: throw RuntimeException("*** $id share not found") // TODO
            val plaintextTallySelection = decryptSelectionWithDecryptionShares(tallySelection, sshares)
            selections[tallySelection.selectionId] = plaintextTallySelection
        }
        return PlaintextTally.Contest(contest.contestId, selections)
    }

    private fun decryptSelectionWithDecryptionShares(
        selection: CiphertextTally.Selection,
        shares: List<PartialDecryption>,
    ): PlaintextTally.Selection {
        if (shares.size != this.nguardians) {
            throw IllegalStateException("decryptSelectionWithDecryptionShares $selection #shares ${shares.size} must equal #guardians ${this.nguardians}")
        }

        // accumulate all of the shares calculated for the selection
        val decryptionShares: Iterable<ElementModP> = shares.map { it.share() }
        val allSharesProductM: ElementModP = with (group) { decryptionShares.multP() }

        // Calculate 𝑀 = 𝐵⁄(∏𝑀𝑖) mod 𝑝. (spec section 3.5.1 eq 10)
        val decryptedValue: ElementModP = selection.ciphertext.data / allSharesProductM
        val dlogM: Int = publicKey.dLog(decryptedValue, maxDlog)?: throw RuntimeException("dlog failed")

        return PlaintextTally.Selection(
            selection.selectionId,
            dlogM,
            decryptedValue,
            selection.ciphertext,
            shares
        )
    }
}