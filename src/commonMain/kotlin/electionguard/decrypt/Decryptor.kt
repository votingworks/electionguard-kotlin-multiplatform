package electionguard.decrypt

import electionguard.ballot.CiphertextTally
import electionguard.ballot.DecryptionShare
import electionguard.ballot.PlaintextTally
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.GroupContext

class Decryptor(val group: GroupContext, val publicKey: ElGamalPublicKey) {

    fun decryptTally(tally: CiphertextTally, shares: Map<String, List<DecryptionShare.DecryptionShareSelection>>): PlaintextTally {
        val contests: MutableMap<String, PlaintextTally.Contest> = HashMap()
        for (tallyContest in tally.contests.values) {
            val plaintextTallyContest = decryptContestWithDecryptionShares(tallyContest, shares)
            contests[tallyContest.contestId] = plaintextTallyContest
        }
        return PlaintextTally(tally.tallyId, contests)
    }

    fun decryptContestWithDecryptionShares(
        contest: CiphertextTally.Contest,
        shares: Map<String, List<DecryptionShare.DecryptionShareSelection>>,
    ): PlaintextTally.Contest {
        val selections: MutableMap<String, PlaintextTally.Selection> = HashMap()
        for (tallySelection in contest.selections.values) {
            val id = "${contest.contestId}#@${tallySelection.selectionId}"
            val sshares = shares.get(id)?: throw RuntimeException("*** $id share not found") // TODO
            val plaintextTallySelection = decryptSelectionWithDecryptionShares(tallySelection, sshares)
            selections[tallySelection.selectionId] = plaintextTallySelection
        }
        return PlaintextTally.Contest(contest.contestId, selections)
    }

    fun decryptSelectionWithDecryptionShares(
        selection: CiphertextTally.Selection,
        shares: List<DecryptionShare.DecryptionShareSelection>,
    ): PlaintextTally.Selection {

        // accumulate all of the shares calculated for the selection
        val decryptionShares: Iterable<ElementModP> = shares.map { it.share }
        val allSharesProductM: ElementModP = with (group) { decryptionShares.multP() }

        // Calculate 𝑀 = 𝐵⁄(∏𝑀𝑖) mod 𝑝.
        val decryptedValue: ElementModP = selection.ciphertext.data / allSharesProductM
        val dlogM: Int = publicKey.dLog(decryptedValue)?: throw RuntimeException("dlog failed")

        return PlaintextTally.Selection(
            selection.selectionId,
            dlogM,
            decryptedValue,
            selection.ciphertext,
            shares
        )
    }
}