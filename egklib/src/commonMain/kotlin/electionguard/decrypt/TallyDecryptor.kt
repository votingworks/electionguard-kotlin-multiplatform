package electionguard.decrypt

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedTally
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.Guardian
import electionguard.ballot.LagrangeCoordinate
import electionguard.ballot.decryptWithBetaToContestData
import electionguard.core.*
import electionguard.keyceremony.calculateGexpPiAtL
import mu.KotlinLogging

private val logger = KotlinLogging.logger("TallyDecryptor")

/** Turn an EncryptedTally into a DecryptedTallyOrBallot. */
class TallyDecryptor(
    val group: GroupContext,
    val qbar: ElementModQ,
    val jointPublicKey: ElGamalPublicKey,
    val lagrangeCoordinates: Map<String, LagrangeCoordinate>,
    val guardians: Map<String, Guardian>, // all the guardians
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
            val shares = decryptions.shares[id] ?: throw IllegalStateException("*** $id share not found")
            decryptSelection(it, shares, contest.contestId, stats)
        }
        return DecryptedTallyOrBallot.Contest(contest.contestId, selections, decryptedContestData)
    }

    private fun decryptContestData(
        where: String,
        contestDataDecryptions: ContestDataResults?, // results for this selection
    ): DecryptedTallyOrBallot.DecryptedContestData? {
        return contestDataDecryptions?.let {
            // response (v) is the sum of the individual responses
            val response: ElementModQ = with(group) { contestDataDecryptions.responses.values.map { it }.addQ() }

            // finally we can create the proof
            if (contestDataDecryptions.challenge == null) {
                logger.error { "$where: ContestDataResults missing challenge" }
                return null
            }
            val challenge = contestDataDecryptions.challenge!!.toElementModQ(group)
            val proof = GenericChaumPedersenProof(challenge, response)

            if (contestDataDecryptions.beta == null) {
                logger.error { "$where: ContestDataResults missing beta" }
                return null
            }
            val contestData =
                contestDataDecryptions.ciphertext.decryptWithBetaToContestData(contestDataDecryptions.beta!!)
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

    // LOOK detect nulls
    private fun decryptSelection(
        selection: EncryptedTally.Selection,
        selectionDecryptions: DecryptionResults, // results for this selection
        contestId: String,
        stats: Stats,
    ): DecryptedTallyOrBallot.Selection {
        // response (v) is the sum of the individual responses, eq 15.
        val response: ElementModQ = with(group) { selectionDecryptions.responses.values.map { it }.addQ() }
        // finally we can create the proof
        val proof = GenericChaumPedersenProof(selectionDecryptions.challenge!!.toElementModQ(group), response)
        val M = selection.ciphertext.data / selectionDecryptions.mbar!! // M = K^t

        val decrypytedSelection = DecryptedTallyOrBallot.Selection(
            selection.selectionId,
            selectionDecryptions.dlogM!!,
            M,
            selection.ciphertext,
            proof
        )

        val startVerifyDetailed = getSystemTimeInMillis()
        if (!selectionDecryptions.checkIndividualResponses()) {
            println("checkIndividualResponses failed for  $contestId and ${selection.selectionId}")
        }
        stats.of("checkIndividualResponses", "selection", "selections").accum(getSystemTimeInMillis() - startVerifyDetailed, 1)

        val startVerify = getSystemTimeInMillis()
        if (!decrypytedSelection.verifySelection()) {
            // println("verifySelection failed for  $contestId and ${selection.selectionId}")
        }
        stats.of("verifySelection", "selection", "selections").accum(getSystemTimeInMillis() - startVerify, 1)

        return decrypytedSelection
    }

    // this is the verifier proof (box 8)
    private var first = false
    private fun DecryptedTallyOrBallot.Selection.verifySelection(): Boolean {
        val Mbar: ElementModP = this.message.data / this.value
        // LOOK these dont agree eq 10
        val a = group.gPowP(this.proof.r) * (jointPublicKey powP this.proof.c) // 8.1
        val b = (this.message.pad powP this.proof.r) * (Mbar powP this.proof.c) // 8.2
        if (first) {
            println(" qbar = $qbar")
            println(" jointPublicKey = $jointPublicKey")
            println(" this.message.pad = ${this.message.pad}")
            println(" this.message.data = ${this.message.data}")
            println(" a= $a")
            println(" b= $b")
            println(" Mbar = $Mbar")
            first = false
        }

        val challenge = hashElements(qbar, jointPublicKey, this.message.pad, this.message.data, a, b, Mbar) // 8.B
        return (challenge.toElementModQ(group) == this.proof.c)
    }

    // Verify with eq 13, 14
    private fun DecryptionResults.checkIndividualResponses(): Boolean {
        var ok = true
        for (partialDecryption in this.shares.values) {
            val guardian = guardians[partialDecryption.guardianId]
                ?: throw IllegalStateException("*** guardian ${partialDecryption.guardianId} not found")
            // val lagrange = lagrangeCoordinates[guardian.guardianId] ?: throw IllegalStateException("*** lagrange not found for ${guardian.guardianId}")
            val vi = this.responses[guardian.guardianId]
                ?: throw IllegalStateException("*** response not found for ${guardian.guardianId}")
            val challenge = this.challenge!!.toElementModQ(group)

            val inner = innerFactor13(guardian.xCoordinate)
            // val middle = guardian.publicKey() * (inner powP lagrange.lagrangeCoefficient)
            val ap = group.gPowP(vi) * (inner powP challenge) // 13
            if (partialDecryption.a != ap) {
                println(" ayes dont match for ${guardian.guardianId}")
                ok = false
            }

            val bp = (this.ciphertext.pad powP vi) * (partialDecryption.mbari powP challenge) // 14
            if (partialDecryption.b != bp) {
                println(" bees dont match for ${guardian.guardianId}")
                ok = false
            }
        }
        return ok
    }

    // the innermost factor of eq 13 LOOK compute ahead of time
    private fun innerFactor13(xcoord: Int): ElementModP =
        with(group) {
            guardians.values.map { calculateGexpPiAtL(xcoord, it.coefficientCommitments()) }.multP()
        }

}