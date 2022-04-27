package electionguard.decrypt

import electionguard.ballot.AvailableGuardian
import electionguard.ballot.CiphertextTally
import electionguard.ballot.DecryptionShare
import electionguard.ballot.DecryptionShare.DecryptionShareContest
import electionguard.ballot.DecryptionShare.PartialDecryption
import electionguard.ballot.PlaintextTally
import electionguard.ballot.TallyResult
import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.toElementModQ

/**
 * Orchestrates the decryption of encrypted Tallies and Ballots with remote Guardians.
 */
class DecryptingMediator(
    val group: GroupContext,
    val context: TallyResult,
    val decryptingTrustees : List<DecryptingTrusteeIF>,
) {

    fun CiphertextTally.decrypt() : PlaintextTally {
        val tallyShares : MutableList<DecryptionShare> = ArrayList()

        for (decryptingTrustee in decryptingTrustees) {
            val share : DecryptionShare = this.computeDecryptionShareForTally(decryptingTrustee)
            tallyShares.add(share)
        }

        // now rearrange for use by Decryptor
        val tallySharesBySelectionId : MutableMap<String, MutableList<PartialDecryption>> = HashMap()
        for (tallyShare in tallyShares) {
            for (tallyContest in tallyShare.contests) {
                for (selection in tallyContest.selections) {
                    val hashId = "${tallyContest.contestId}#@${selection.selectionId}"
                    var slist : MutableList<PartialDecryption>? = tallySharesBySelectionId.get(hashId)
                    if (slist == null) {
                        slist = ArrayList()
                        tallySharesBySelectionId.put(hashId, slist)
                    }
                    slist.add(selection)
                }
            }
        }

        val decryptor = Decryptor(group, context.jointPublicKey())
        return decryptor.decryptTally(this, tallySharesBySelectionId)
    }

 /**
   * Compute a guardian's share of a decryption for the tally, aka a 'partial decyrption'.
   * <p>
   * @param guardian: The guardian who will partially decrypt the tally
   * @param tally: The election tally to decrypt
   * @param context: The public election encryption context
   * @return a DecryptionShare
   */
 fun CiphertextTally.computeDecryptionShareForTally(
     guardian: DecryptingTrusteeIF,
 ): DecryptionShare {

     // Get all the Ciphertext that need to be decrypted, and do so in one call
     val texts: MutableList<ElGamalCiphertext> = ArrayList()
     for (tallyContest in this.contests.values) {
         for (selection in tallyContest.selections.values) {
             texts.add(selection.ciphertext);
         }
     }
     // returned in order
     val results: List<PartialDecryptionProof> =
         guardian.partialDecrypt(group, texts, context.cryptoExtendedBaseHash(), null)

     // Create the guardian's DecryptionShare for the tally
     var count = 0;
     val contests : MutableList<DecryptionShareContest> = ArrayList()
     for (tallyContest in this.contests.values) {
         val selections : MutableList<PartialDecryption> = ArrayList()
         for (selection in tallyContest.selections.values) {
             val proof: PartialDecryptionProof = results.get(count);
             selections.add(
                 PartialDecryption(
                     selection.selectionId,
                     guardian.id(),
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
                 guardian.id(),
                 tallyContest.contestDescriptionHash.toElementModQ(group),
                 selections
             )
         )
     }

     return DecryptionShare(
         guardian.id(),
         guardian.electionPublicKey(),
         contests
     )
 }

    // Compute lagrange coefficients for each of the available guardians
    fun computeAvailableGuardians() : List<AvailableGuardian> {
        val result = ArrayList<AvailableGuardian>()
        for (decryptingTrustee in decryptingTrustees) {
            val seq_orders: List<Int> = decryptingTrustees
                .filter { !it.id().equals(decryptingTrustee.id()) }
                .map { it.xCoordinate() }
            val coeff: ElementModQ = computeLagrangeCoefficient(decryptingTrustee.xCoordinate(), seq_orders)
            result.add(AvailableGuardian(decryptingTrustee.id(), decryptingTrustee.xCoordinate(), coeff))
        }
        return result
    }

    fun computeLagrangeCoefficient(coordinate: Int, degrees: List<Int>): ElementModQ {
        val numerator: Int = degrees.reduce { a, b -> a * b }

        val diff: List<Int> = degrees.map { degree: Int -> degree - coordinate }
        val denominator = diff.reduce { a, b -> a * b }

        val denomQ = if (denominator > 0) denominator.toElementModQ(group) else (-denominator).toElementModQ(group).unaryMinus()

        return numerator.toElementModQ(group) / denomQ
    }
}