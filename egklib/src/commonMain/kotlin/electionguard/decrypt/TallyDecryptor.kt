package electionguard.decrypt

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedTally
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.LagrangeCoordinate
import electionguard.ballot.decryptWithBetaToContestData
import electionguard.core.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger("TallyDecryptor")

/** Turn an EncryptedTally into a DecryptedTallyOrBallot. */
class TallyDecryptor(
    val group: GroupContext,
    val extendedBaseHash: ElementModQ,
    val jointPublicKey: ElGamalPublicKey,
    val lagrangeCoordinates: Map<String, LagrangeCoordinate>,
    val guardians: Guardians, // all the guardians
) {
    /**
     * Called after gathering the shares and challenge responses for all available trustees.
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
        if (first) { // temp debug
            println(" extendedBaseHash = $extendedBaseHash")
            println(" jointPublicKey = $jointPublicKey")
            println(" this.message.pad = ${this.message.pad}")
            println(" this.message.data = ${this.message.data}")
            println(" a= $a")
            println(" b= $b")
            println(" Mbar = $Mbar")
            first = false
        }

        // The challenge value c satisfies c = H(HE ; 30, K, A, B, a, b, M ). 8.B, eq 72
        val challenge = hashFunction(extendedBaseHash.byteArray(), 0x30.toByte(), jointPublicKey.key, this.message.pad, this.message.data, a, b, Mbar)
        return (challenge.toElementModQ(group) == this.proof.c)
    }

    // Verify with eq 63, 64
    private fun DecryptionResults.checkIndividualResponses(): Boolean {
        var ok = true
        for (partialDecryption in this.shares.values) {
            val guardianId = partialDecryption.guardianId
            val vi = this.responses[guardianId]
                ?: throw IllegalStateException("*** response not found for ${guardianId}")
            val wi = lagrangeCoordinates[guardianId]!!.lagrangeCoefficient
            val challenge = wi * this.challenge!!.toElementModQ(group) // eq 61

            val inner = guardians.getGexpP(guardianId)
            val ap = group.gPowP(vi) * (inner powP challenge) // eq 63
            if (partialDecryption.a != ap) {
                println(" ayes dont match for ${guardianId}")
                ok = false
            }

            val bp = (this.ciphertext.pad powP vi) * (partialDecryption.mbari powP challenge) // eq 64
            if (partialDecryption.b != bp) {
                println(" bees dont match for ${guardianId}")
                ok = false
            }
        }
        return ok
    }
}