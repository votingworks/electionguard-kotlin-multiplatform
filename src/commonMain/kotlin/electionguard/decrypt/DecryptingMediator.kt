package electionguard.decrypt

import electionguard.ballot.AvailableGuardian
import electionguard.ballot.CiphertextTally
import electionguard.decrypt.DecryptionShare.DecryptionShareContest
import electionguard.decrypt.DecryptionShare.PartialDecryption
import electionguard.ballot.PlaintextTally
import electionguard.ballot.TallyResult
import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.toElementModQ

/**
 * Orchestrates the decryption of encrypted Tallies and Ballots with DecryptingTrustee's.
 */
class DecryptingMediator(
    val group: GroupContext,
    val tallyResult: TallyResult,
    val decryptingTrustees: List<DecryptingTrusteeIF>,
) {

    fun CiphertextTally.decrypt(): PlaintextTally {
        val tallyShares: MutableList<DecryptionShare> = ArrayList()

        for (decryptingTrustee in decryptingTrustees) {
            val share: DecryptionShare = this.computeDecryptionShareForTally(decryptingTrustee)
            tallyShares.add(share)
        }

        // now rearrange for use by Decryptor
        val tallySharesBySelectionId: MutableMap<String, MutableList<PartialDecryption>> = HashMap()
        for (tallyShare in tallyShares) {
            for (tallyContest in tallyShare.contests) {
                for (selection in tallyContest.selections) {
                    val hashId = "${tallyContest.contestId}#@${selection.selectionId}"
                    var slist: MutableList<PartialDecryption>? = tallySharesBySelectionId.get(hashId)
                    if (slist == null) {
                        slist = ArrayList()
                        tallySharesBySelectionId.put(hashId, slist)
                    }
                    slist.add(selection)
                }
            }
        }

        val decryptor = Decryptor(group, tallyResult.jointPublicKey())
        return decryptor.decryptTally(this, tallySharesBySelectionId)
    }

    /**
     * Compute a guardian's share of a decryption for the tally, aka a 'partial decyrption'.
     * <p>
     * @param trustee: The guardian who will partially decrypt the tally
     * @return a DecryptionShare
     */
    fun CiphertextTally.computeDecryptionShareForTally(
        trustee: DecryptingTrusteeIF,
    ): DecryptionShare {

        // Get all the Ciphertext that need to be decrypted in one call
        val texts: MutableList<ElGamalCiphertext> = ArrayList()
        for (tallyContest in this.contests.values) {
            for (selection in tallyContest.selections.values) {
                texts.add(selection.ciphertext);
            }
        }
        // returned in order
        val results: List<PartialDecryptionAndProof> =
            trustee.partialDecrypt(group, texts, tallyResult.cryptoExtendedBaseHash(), null)

        // Create the guardian's DecryptionShare for the tally
        var count = 0;
        val contests: MutableList<DecryptionShareContest> = ArrayList()
        for (tallyContest in this.contests.values) {
            val selections: MutableList<PartialDecryption> = ArrayList()
            for (selection in tallyContest.selections.values) {
                val proof: PartialDecryptionAndProof = results.get(count);
                selections.add(
                    PartialDecryption(
                        selection.selectionId,
                        trustee.id(),
                        proof.partialDecryption,
                        proof.proof,
                        null
                    )
                )
                count++
            }
            contests.add(
                DecryptionShareContest(
                    tallyContest.contestId,
                    trustee.id(),
                    tallyContest.contestDescriptionHash.toElementModQ(group),
                    selections
                )
            )
        }

        return DecryptionShare(
            trustee.id(),
            trustee.electionPublicKey(),
            contests
        )
    }

    // LOOK who's missing ??
    // Compute lagrange coefficients for each of the available guardians
    fun computeAvailableGuardians(): List<AvailableGuardian> {
        val result = ArrayList<AvailableGuardian>()
        for (otherTrustee in decryptingTrustees) {
            val seq_orders: List<UInt> = decryptingTrustees.filter { !it.id().equals(otherTrustee.id()) }.map { it.xCoordinate() }
            val coeff: ElementModQ = group.computeLagrangeCoefficient(otherTrustee.xCoordinate(), seq_orders)
            result.add(AvailableGuardian(otherTrustee.id(), otherTrustee.xCoordinate().toInt(), coeff))
        }
        return result
    }
}

fun GroupContext.computeLagrangeCoefficient(coordinate: UInt, present: List<UInt>): ElementModQ {
    val others: List<UInt> = present.filter { !it.equals(coordinate) }
    val numerator: Int = others.reduce { a, b -> a * b }.toInt()

    val diff: List<Int> = others.map { degree -> degree.toInt() - coordinate.toInt() }
    val denominator = diff.reduce { a, b -> a * b }

    val denomQ =
        if (denominator > 0) denominator.toElementModQ(this) else (-denominator).toElementModQ(this).unaryMinus()

    return numerator.toElementModQ(this) / denomQ
}