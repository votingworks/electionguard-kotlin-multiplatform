package electionguard.decrypt

import electionguard.ballot.CiphertextTally
import electionguard.ballot.PlaintextTally
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.GroupContext

class TallyDecryptor(val group: GroupContext, val publicKey: ElGamalPublicKey, val nguardians: Int) {

    //         val sharesBySelectionId: MutableMap<String, MutableList<Decryption>> = HashMap()
    fun decryptTally(tally: CiphertextTally, shares: Map<String, List<PartialDecryption>>): PlaintextTally {
        val contests: MutableMap<String, PlaintextTally.Contest> = HashMap()
        for (tallyContest in tally.contests.values) {
            val plaintextTallyContest = decryptContestWithDecryptionShares(tallyContest, shares)
            contests[tallyContest.contestId] = plaintextTallyContest
        }
        return PlaintextTally(tally.tallyId, contests)
    }

    fun decryptContestWithDecryptionShares(
        contest: CiphertextTally.Contest,
        shares: Map<String, List<PartialDecryption>>,
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
        shares: List<PartialDecryption>,
    ): PlaintextTally.Selection {
        if (shares.size != this.nguardians) {
            throw IllegalStateException("decryptSelectionWithDecryptionShares $selection ${shares.size} != ${this.nguardians}")
        }

        // accumulate all of the shares calculated for the selection
        val decryptionShares: Iterable<ElementModP> = shares.map { it.share() }
        val allSharesProductM: ElementModP = with (group) { decryptionShares.multP() }

        // Calculate 𝑀 = 𝐵⁄(∏𝑀𝑖) mod 𝑝. (spec section 3.5.1 eq 10)
        val decryptedValue: ElementModP = selection.ciphertext.data / allSharesProductM
        val dlogM: Int = publicKey.dLog(decryptedValue, 100)?: throw RuntimeException("dlog failed")

        return PlaintextTally.Selection(
            selection.selectionId,
            dlogM,
            decryptedValue,
            selection.ciphertext,
            shares
        )
    }
}