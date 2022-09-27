package electionguard.decrypt

import electionguard.ballot.DecryptingGuardian
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.EncryptedTally
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.toElementModQ

/**
 * Orchestrates the decryption of encrypted Tallies and Ballots with DecryptingTrustees.
 */
class Decryption(
    val group: GroupContext,
    val init: ElectionInitialized,
    private val decryptingTrustees: List<DecryptingTrusteeIF>,
    private val missingTrustees: List<String>,
) {
    val availableGuardians: List<DecryptingGuardian> by lazy {
        val result = ArrayList<DecryptingGuardian>()
        for (otherTrustee in decryptingTrustees) {
            val present: List<Int> =
                decryptingTrustees.filter { it.id() != otherTrustee.id() }.map { it.xCoordinate() }
            val coeff: ElementModQ = group.computeLagrangeCoefficient(otherTrustee.xCoordinate(), present)
            result.add(DecryptingGuardian(otherTrustee.id(), otherTrustee.xCoordinate(), coeff))
        }
        // sorted by guardianId, to match PartialDecryption.lagrangeInterpolation()
        result.sortedBy { it.guardianId}
    }

    fun decryptBallot(ballot: EncryptedBallot): DecryptedTallyOrBallot {
        // pretend a ballot is a tally
        return ballot.convertToTally().decrypt()
    }

    fun EncryptedTally.decrypt(): DecryptedTallyOrBallot {
        val shares: MutableList<DecryptionShare> = mutableListOf() // one for each trustee
        for (decryptingTrustee in decryptingTrustees) {
            val share: DecryptionShare = this.computeDecryptionShareForTrustee(decryptingTrustee)
            shares.add(share)
        }

        // now rearrange for use by TallyDecryptor
        val sharesBySelectionId: MutableMap<String, MutableList<PartialDecryption>> = HashMap()
        for (tallyShare in shares) {
            tallyShare.directDecryptions.entries.map { (selectionId, partial) ->
                var smap: MutableList<PartialDecryption>? = sharesBySelectionId[selectionId]
                if (smap == null) {
                    smap = mutableListOf()
                    sharesBySelectionId[selectionId] = smap
                }
                smap.add(PartialDecryption(tallyShare.decryptingTrustee, partial))
            }
        }

        for (tallyShare in shares) {
            tallyShare.compensatedDecryptions.entries.map {(selectionId, haveDecrypt) ->
                var cmap: MutableList<PartialDecryption>? = sharesBySelectionId[selectionId]
                if (cmap == null) {
                    cmap = mutableListOf()
                    sharesBySelectionId[selectionId] = cmap
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
            // compute missing shares with lagrange interpolation
            sharesBySelectionId.values.flatten().forEach { it.lagrangeInterpolation(availableGuardians) }
        }

        // After gathering the shares for all guardians (partial or compensated), we can decrypt.
        val decryptor = TallyDecryptor(group, init.jointPublicKey(), init.numberOfGuardians())
        return decryptor.decryptTally(this, sharesBySelectionId)
    }

    /**
     * Compute a guardian's share of a decryption, aka a 'partial decryption'.
     * @param trustee: The guardian who will partially decrypt the tally
     * @return a DecryptionShare for this trustee
     */
    private fun EncryptedTally.computeDecryptionShareForTrustee(
        trustee: DecryptingTrusteeIF,
    ): DecryptionShare {

        // Get all the Ciphertext that need to be decrypted in one call
        val texts: MutableList<ElGamalCiphertext> = mutableListOf()
        for (tallyContest in this.contests) {
            for (selection in tallyContest.selections) {
                texts.add(selection.ciphertext)
            }
        }

        // LOOK could ask for all direct and compensated in one call, so only one call to each trustee,
        //  instead of 1 + nmissing

        // direct decryptions
        val partialDecryptions: List<DirectDecryptionAndProof> =
            trustee.directDecrypt(group, texts, init.cryptoExtendedBaseHash(), null)

        // Place the results into the DecryptionShare
        val decryptionShare = DecryptionShare(trustee.id())
        var count = 0
        for (tallyContest in this.contests) {
            for (tallySelection in tallyContest.selections) {
                val proof: DirectDecryptionAndProof = partialDecryptions[count]
                val partialDecryption = DirectDecryption(
                    tallySelection.selectionId,
                    trustee.id(),
                    proof.partialDecryption,
                    proof.proof,
                )
                decryptionShare.addDirectDecryption(
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
                trustee.compensatedDecrypt(group, missing, texts, init.cryptoExtendedBaseHash(), null)

            // Place the results into the DecryptionShare
            var count2 = 0
            for (tallyContest in this.contests) {
                for (tallySelection in tallyContest.selections) {
                    val proof: CompensatedDecryptionAndProof = compensatedDecryptions[count2]
                    val recoveredDecryption = RecoveredPartialDecryption(
                        trustee.id(),
                        missing,
                        proof.partialDecryption,
                        proof.recoveredPublicKeyShare,
                        proof.proof
                    )
                    decryptionShare.addRecoveredDecryption(
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

/** Compute the lagrange coefficient, now that we know which guardians are present. spec 1.51 section 3.5.2, eq 64. */
fun GroupContext.computeLagrangeCoefficient(coordinate: Int, present: List<Int>): ElementModQ {
    val others: List<Int> = present.filter { it != coordinate }
    val numerator: Int = others.reduce { a, b -> a * b }

    val diff: List<Int> = others.map { degree -> degree - coordinate }
    val denominator = diff.reduce { a, b -> a * b }

    val denomQ =
        if (denominator > 0) denominator.toElementModQ(this) else (-denominator).toElementModQ(this)
            .unaryMinus()

    return numerator.toElementModQ(this) / denomQ
}

/** Convert an EncryptedBallot to an EncryptedTally, for processing spoiled ballots. */
private fun EncryptedBallot.convertToTally(): EncryptedTally {
    val contests = this.contests.map { contest ->
        // remove placeholders
        val selections = contest.selections.filter { !it.isPlaceholderSelection }.map {
            EncryptedTally.Selection(it.selectionId, it.sequenceOrder, it.selectionHash, it.ciphertext)
        }
        EncryptedTally.Contest(contest.contestId, contest.sequenceOrder, contest.contestHash, selections)
    }
    return EncryptedTally(this.ballotId, contests)

}