package electionguard.decrypt

import electionguard.ballot.EncryptedTally
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.GroupContext

// TODO Use a configuration to set to the maximum possible vote. Keep low for testing to detect bugs quickly.
private const val maxDlog: Int = 1000

/** Decrypt an EncryptedTally into a DecryptedTallyOrBallot. */
class TallyDecryptor(val group: GroupContext, val jointPublicKey: ElGamalPublicKey, private val nguardians: Int) {

    /**
     * After gathering the shares for all guardians (partial or compensated), we can decrypt the tally.
     * Shares are in a Map keyed by "${contestId}#@${selectionId}"
     */
    fun decryptTally(tally: EncryptedTally, shares: Map<String, List<PartialDecryption>>): DecryptedTallyOrBallot {
        val contests: MutableMap<String, DecryptedTallyOrBallot.Contest> = HashMap()
        for (tallyContest in tally.contests) {
            val decryptedContest = decryptContest(tallyContest, shares)
            contests[tallyContest.contestId] = decryptedContest
        }
        return DecryptedTallyOrBallot(tally.tallyId, contests)
    }

    private fun decryptContest(
        contest: EncryptedTally.Contest,
        shares: Map<String, List<PartialDecryption>>,
    ): DecryptedTallyOrBallot.Contest {
        val selections: MutableMap<String, DecryptedTallyOrBallot.Selection> = HashMap()
        for (tallySelection in contest.selections) {
            val id = "${contest.contestId}#@${tallySelection.selectionId}"
            val sshares = shares[id] ?: throw IllegalStateException("*** $id share not found") // TODO something better?
            val decryptedSelection = decryptSelection(tallySelection, sshares, contest.contestId)
            selections[tallySelection.selectionId] = decryptedSelection
        }
        return DecryptedTallyOrBallot.Contest(contest.contestId, selections, null)
    }

    private fun decryptSelection(
        selection: EncryptedTally.Selection,
        shares: List<PartialDecryption>,
        contestId: String,
    ): DecryptedTallyOrBallot.Selection {
        if (shares.size != this.nguardians) {
            throw IllegalStateException("decryptSelection $selection #shares ${shares.size} must equal #guardians ${this.nguardians}")
        }

        // accumulate all of the shares calculated for the selection
        val decryptionShares: Iterable<ElementModP> = shares.map { it.share() }
        val allSharesProductM: ElementModP = with (group) { decryptionShares.multP() }

        // Calculate 𝑀 = 𝐵⁄(∏𝑀𝑖) mod 𝑝. (spec 1.51 section 3.5.1 eq 58)
        val decryptedValue: ElementModP = selection.ciphertext.data / allSharesProductM
        // Now we know M, and since 𝑀 = K^t mod 𝑝, t = logK (M) (note version 1 has 𝑀 = g^t)
        val dlogM: Int = jointPublicKey.dLog(decryptedValue, maxDlog) ?:
                throw RuntimeException("dlog failed on ${contestId} / ${selection.selectionId}")

        return DecryptedTallyOrBallot.Selection(
            selection.selectionId,
            dlogM,
            decryptedValue,
            selection.ciphertext,
            shares
        )
    }
}