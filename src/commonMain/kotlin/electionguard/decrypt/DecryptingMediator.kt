package electionguard.decrypt

import electionguard.ballot.AvailableGuardian
import electionguard.ballot.CiphertextTally
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
    val missingTrustees: List<String>,
) {
    val availableGuardians: List<AvailableGuardian> by lazy {
        val result = ArrayList<AvailableGuardian>()
        for (otherTrustee in decryptingTrustees) {
            val present: List<UInt> =
                decryptingTrustees.filter { !it.id().equals(otherTrustee.id()) }.map { it.xCoordinate() }
            val coeff: ElementModQ = group.computeLagrangeCoefficient(otherTrustee.xCoordinate(), present)
            result.add(AvailableGuardian(otherTrustee.id(), otherTrustee.xCoordinate().toInt(), coeff))
        }
        // sorted by guardianId, to match PartialDecryption.lagrangeInterpolation()
        result.sortedBy { it.guardianId}
    }

    fun CiphertextTally.decrypt(): PlaintextTally {
        val shares: MutableList<DecryptionShare> = ArrayList()
        for (decryptingTrustee in decryptingTrustees) {
            val share: DecryptionShare = this.computePartialDecryptionForTally(decryptingTrustee)
            shares.add(share)
        }

        // now rearrange for use by Decryptor
        val sharesBySelectionId: MutableMap<String, MutableList<PartialDecryption>> = HashMap()
        for (tallyShare in shares) {
            tallyShare.partialDecryptions.entries.map { (selectionId, partial) ->
                var smap: MutableList<PartialDecryption>? = sharesBySelectionId.get(selectionId)
                if (smap == null) {
                    smap = mutableListOf()
                    sharesBySelectionId.put(selectionId, smap)
                }
                smap.add(PartialDecryption(tallyShare.decryptingTrustee, partial))
            }
        }

        for (tallyShare in shares) {
            tallyShare.compensatedDecryptions.entries.map {(selectionId, haveDecrypt) ->
                var cmap: MutableList<PartialDecryption>? = sharesBySelectionId.get(selectionId)
                if (cmap == null) {
                    cmap = mutableListOf()
                    sharesBySelectionId.put(selectionId, cmap)
                }
                // distribute the recoveredDecryptions for this trustee across the missing guardians
                haveDecrypt.missingDecryptions.values.map { recovered ->
                    var wantDecrypt = cmap.find { it.guardianId == recovered.missingGuardianId}
                    if (wantDecrypt == null) {
                        wantDecrypt = PartialDecryption(recovered.missingGuardianId, haveDecrypt)
                        cmap.add(wantDecrypt)
                    }
                    wantDecrypt.add(recovered)
                }
            }
        }

        if (missingTrustees.isNotEmpty()) {
            // compute Missing Shares with lagrange interpolation
            sharesBySelectionId.values.flatten().forEach { it.lagrangeInterpolation(availableGuardians) }
        }

        val decryptor = TallyDecryptor(group, tallyResult.jointPublicKey(), tallyResult.numberOfGuardians())
        return decryptor.decryptTally(this, sharesBySelectionId)
    }

    /**
     * Compute a guardian's share of a decryption, aka a 'partial decryption'.
     * @param trustee: The guardian who will partially decrypt the tally
     * @return a DecryptionShare for this trustee
     */
    fun CiphertextTally.computePartialDecryptionForTally(
        trustee: DecryptingTrusteeIF,
    ): DecryptionShare {

        // Get all the Ciphertext that need to be decrypted in one call
        val texts: MutableList<ElGamalCiphertext> = mutableListOf()
        for (tallyContest in this.contests.values) {
            for (selection in tallyContest.selections.values) {
                texts.add(selection.ciphertext);
            }
        }

        // LOOK could ask for all direct and compensated in one call,  so only one call to each trustee,
        //  instead of 1 + nmissing

        // direct decryptions
        val partialDecryptions: List<DirectDecryptionAndProof> =
            trustee.partialDecrypt(group, texts, tallyResult.cryptoExtendedBaseHash(), null)

        // Place the results into the DecryptionShare
        val decryptionShare = DecryptionShare(trustee.id())
        var count = 0;
        for (tallyContest in this.contests.values) {
            for (tallySelection in tallyContest.selections.values) {
                val proof: DirectDecryptionAndProof = partialDecryptions.get(count);
                val partialDecryption = DirectDecryption(
                    tallySelection.selectionId,
                    trustee.id(),
                    proof.partialDecryption,
                    proof.proof,
                )
                decryptionShare.addPartialDecryption(
                    tallyContest.contestId,
                    tallySelection.selectionId,
                    partialDecryption
                )
                count++
            }
        }

        // compensated decryptions
        for (missing in missingTrustees) {
            val compensatedDecryptions: List<CompensatedDecryptionAndProof> =
                trustee.compensatedDecrypt(group, missing, texts, tallyResult.cryptoExtendedBaseHash(), null)

            // Place the results into the DecryptionShare
            var count2 = 0;
            for (tallyContest in this.contests.values) {
                for (tallySelection in tallyContest.selections.values) {
                    val proof: CompensatedDecryptionAndProof = compensatedDecryptions.get(count2);
                    val recoveredDecryption = MissingPartialDecryption(
                        trustee.id(),
                        missing,
                        proof.partialDecryption,
                        proof.recoveredPublicKeyShare,
                        proof.proof
                    )
                    decryptionShare.addMissingDecryption(
                        tallyContest.contestId,
                        tallySelection.selectionId,
                        missing,
                        recoveredDecryption
                    )
                    count2++
                }
            }
        }

        return decryptionShare
    }
}

fun GroupContext.computeLagrangeCoefficient(coordinate: UInt, present: List<UInt>): ElementModQ {
    val others: List<UInt> = present.filter { !it.equals(coordinate) }
    val numerator: Int = others.reduce { a, b -> a * b }.toInt()

    val diff: List<Int> = others.map { degree -> degree.toInt() - coordinate.toInt() }
    val denominator = diff.reduce { a, b -> a * b }

    val denomQ =
        if (denominator > 0) denominator.toElementModQ(this) else (-denominator).toElementModQ(this)
            .unaryMinus()

    return numerator.toElementModQ(this) / denomQ
}