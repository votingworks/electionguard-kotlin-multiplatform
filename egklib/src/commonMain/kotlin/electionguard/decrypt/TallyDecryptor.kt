package electionguard.decrypt

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedTally
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.Guardian
import electionguard.ballot.LagrangeCoordinate
import electionguard.ballot.decryptWithBetaToContestData
import electionguard.core.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger("TallyDecryptor")

/** Turn an EncryptedTally into a DecryptedTallyOrBallot. */
class TallyDecryptor(
    val group: GroupContext,
    val qbar: ElementModQ,
    val jointPublicKey: ElGamalPublicKey,
    val lagrangeCoordinates: Map<String, LagrangeCoordinate>,
    val guardians: Map<String, Guardian>,
    val missingTrustees: List<String>, // ids of the missing trustees
) {
    /**
     * Called after gathering the shares for all available trustees.
     * Shares are in a Map keyed by "${contestId}#@${selectionId}"
     */
    fun decryptTally(
        tally: EncryptedTally,
        decryptions: Decryptions,
        stats: Stats,
    ): DecryptedTallyOrBallot {
        // LOOK could parallelize contests
        val contests = tally.contests.map { decryptContest(it, decryptions, stats) }
        return DecryptedTallyOrBallot(tally.tallyId, contests)
    }

    private fun decryptContest(
        contest: EncryptedTally.Contest,
        decryptions: Decryptions,
        stats: Stats,
    ): DecryptedTallyOrBallot.Contest {
        val results = decryptions.contestData[contest.contestId]
        val decryptedContestData = decryptContestData(contest.contestId, results)

        val selections = contest.selections.map {
            val id = "${contest.contestId}#@${it.selectionId}"
            val shares = decryptions.shares[id]
                ?: throw IllegalStateException("*** $id share not found") // TODO something better?
            decryptSelection(it, shares, contest.contestId, stats)
        }
        return DecryptedTallyOrBallot.Contest(contest.contestId, selections, decryptedContestData)
    }

    private fun decryptContestData(
        where: String,
        contestDataDecryptions: ContestDataResults?, // results for this selection
    ): DecryptedTallyOrBallot.DecryptedContestData? {
        return contestDataDecryptions?.let {
            // response is the sum of the individual responses
            val response: ElementModQ = with(group) { contestDataDecryptions.responses.values.map { it }.addQ() }

            // finally we can create the proof
            if (contestDataDecryptions.challenge == null) {
                logger.error { "$where: ContestDataResults missing challenge"}
                return null
            }
            val challenge = contestDataDecryptions.challenge!!.toElementModQ(group)
            val proof = GenericChaumPedersenProof(challenge, response)

            if (contestDataDecryptions.beta == null) {
                logger.error { "$where: ContestDataResults missing beta"}
                return null
            }
            val contestData = contestDataDecryptions.ciphertext.decryptWithBetaToContestData(contestDataDecryptions.beta!!)
            if (contestData is Err) {
                return null
            }

            DecryptedTallyOrBallot.DecryptedContestData(
                contestData.unwrap(),
                contestDataDecryptions.ciphertext,
                proof,
                contestDataDecryptions.beta!!,
            )
        }
    }

    private fun decryptSelection(
        selection: EncryptedTally.Selection,
        selectionDecryptions: DecryptionResults, // results for this selection
        contestId: String,
        stats: Stats,
    ): DecryptedTallyOrBallot.Selection {
        // response is the sum of the individual responses
        val response: ElementModQ = with(group) { selectionDecryptions.responses.values.map { it }.addQ() }
        // finally we can create the proof
        val proof = GenericChaumPedersenProof(selectionDecryptions.challenge!!.toElementModQ(group), response)

        val decrypytedSelection = DecryptedTallyOrBallot.Selection(
            selection.selectionId,
            selectionDecryptions.dlogM!!,
            selectionDecryptions.M!!,
            selection.ciphertext,
            proof
        )

        val startVerify = getSystemTimeInMillis()
        if (!decrypytedSelection.verifySelection()) {
            println("verifySelection failed for  $contestId and ${selection.selectionId}")
        }
        stats.of("verifySelection", "selection", "selections").accum(getSystemTimeInMillis() - startVerify, 1)
        val startVerifyDetailed = getSystemTimeInMillis()
        if (!selectionDecryptions.detailedVerify()) {
            println("detailedVerify failed for  $contestId and ${selection.selectionId}")
        }
        stats.of("detailedVerify", "selection", "selections").accum(getSystemTimeInMillis() - startVerifyDetailed, 1)
        val startVerifyGuardians = getSystemTimeInMillis()
        if (!selectionDecryptions.guardianVerify()) {
            println("guardianVerify failed for  $contestId and ${selection.selectionId}")
        }
        stats.of("guardianVerify", "guardians", "selections").accum(getSystemTimeInMillis() - startVerifyGuardians, selectionDecryptions.shares.size)

        return decrypytedSelection
    }

    // this is the verifier proof (box 8)
    private fun DecryptedTallyOrBallot.Selection.verifySelection(): Boolean {
        val Mbar: ElementModP = this.message.data / this.value
        val a = group.gPowP(this.proof.r) * (jointPublicKey powP this.proof.c) // 8.1
        val b = (this.message.pad powP this.proof.r) * (Mbar powP this.proof.c) // 8.2

        val challenge = hashElements(qbar, jointPublicKey, this.message.pad, this.message.data, a, b, this.value) // 8.B
        return (challenge.toElementModQ(group) == this.proof.c)
    }

    // verify each guardian results (modified box 8)
    // version 1.03
    //  (8.A) The given value vi is in the set Zq.
    //  (8.B) The given values ai and bi are both in the set Zr.
    //  (8.C) Use c = challenge value from new spec.
    //  (8.D) The equation g^vi mod p = (ai / Ki^c) mod p
    //  (8.E) The equation A^vi mod p = (bi / Mi^c) mod p
    //
    // version 1.52, 9.D
    //
    //   ti = si + wi * (SumV (Pj(i)), Sum over missing guardians in V
    //  vil = uil - cil * ti
    //      = uil - cil * (si + wi * (SumV (Pj(i)))
    //
    // g^vil = g^uil / g^(cil * (si + wi * (SumV (Pj(i))))
    //       = ail / (g^si * Prod(g^Pj(i))^wi)^cil
    //       = ail / (Ki * Prod(g^Pj(i))^wi)^cil
    private fun DecryptionResults.guardianVerify(): Boolean {
        var ok = true
        this.shares.forEach { (guardianId, share) ->
            val guardian = guardians[guardianId] ?: throw IllegalStateException("*** guardian ${guardianId} not found")
            val lagrange = lagrangeCoordinates[guardianId] ?: throw IllegalStateException("*** lagrangeCoordinates for ${guardianId} not found")
            val vi = this.responses[guardianId] ?: throw IllegalStateException("*** response for ${guardianId} not found")
            val c = this.challenge!!.toElementModQ(group)

            val extKi = if (missingTrustees.isEmpty()) guardian.publicKey() else {
                // List(g^Pj(i))
                val gpils: Iterable<ElementModP> = missingTrustees.map {
                    val missing = guardians[it] ?: throw IllegalStateException("*** missing guardian ${it} not found")
                    calculateGexpPiAtL(guardian.xCoordinate, missing.coefficientCommitments())
                }
                val prod = with(group) { gpils.multP() } // Prod(g^Pj(i))
                val prodExpW = prod powP lagrange.lagrangeCoefficient // Prod(g^Pj(i))^wi
                guardian.publicKey() * prodExpW // Ki * Prod(g^Pj(i))^wi
            }

            if (group.gPowP(vi) != share.a / (extKi powP c)) { // 9.D
                println("9D failed for ${guardianId}")
                ok = false
            }
            if ((this.ciphertext.pad powP vi) != (share.b / (share.mbari powP c))) { // 8.E
                println("9E failed for ${guardianId}")
                ok = false
            }
        }
        return ok
    }

    // Verify with eq 64 and 65
    private fun DecryptionResults.detailedVerify(): Boolean {
        var ok = true
        for (partialDecryption in this.shares.values) {
            val guardian = guardians[partialDecryption.guardianId]
                ?: throw IllegalStateException("*** guardian ${partialDecryption.guardianId} not found")
            val lagrange = lagrangeCoordinates[guardian.guardianId]
                ?: throw IllegalStateException("*** lagrange not found for ${guardian.guardianId}")
            val vi = this.responses[guardian.guardianId]
                ?: throw IllegalStateException("*** response not found for ${guardian.guardianId}")
            val challenge = this.challenge!!.toElementModQ(group)

            val inner = innerFactor64(guardian.xCoordinate)
            val middle = guardian.publicKey() * (inner powP lagrange.lagrangeCoefficient)
            val ap = group.gPowP(vi) * (middle powP challenge) // 64
            if (partialDecryption.a != ap) {
                println("ayes dont match for ${guardian.guardianId}")
                ok = false
            }

            val bp = (this.ciphertext.pad powP vi)  * (partialDecryption.mbari powP challenge) // 65
            if (partialDecryption.b != bp) {
                println("bees dont match for ${guardian.guardianId}")
                ok = false
            }
        }
        return ok
    }

    // the innermost factor of eq 64
    private fun innerFactor64(xcoord: Int): ElementModP {
        val trusteeNames = lagrangeCoordinates.values.map { it.guardianId }.toSet()
        val missingGuardians = guardians.values.filter { !trusteeNames.contains(it.guardianId) }
        return if (missingGuardians.isEmpty()) {
            group.ONE_MOD_P
        } else {
            with(group) {
                missingGuardians.map { calculateGexpPiAtL(xcoord, it.coefficientCommitments()) }.multP()
            }
        }
    }

    /**
     * Calculate g^Pi(ℓ) mod p = Product ((K_i,j)^ℓ^j) mod p, j = 0, quorum-1
     * Used in KeyCeremonyTrustee and DecryptingTrustee, public information.
     * use the one in ElectionPolynomial
     */
    fun calculateGexpPiAtL(
        xcoord: Int,  // l
        coefficientCommitments: List<ElementModP>  // the committments to Pi
    ): ElementModP {
        val group = compatibleContextOrFail(*coefficientCommitments.toTypedArray())
        val xcoordQ: ElementModQ = group.uIntToElementModQ(xcoord.toUInt())
        var result: ElementModP = group.ONE_MOD_P
        var xcoordPower: ElementModQ = group.ONE_MOD_Q // ℓ^j

        for (commitment in coefficientCommitments) {
            val term = commitment powP xcoordPower // (K_i,j)^ℓ^j
            result *= term
            xcoordPower *= xcoordQ
        }
        return result
    }

}