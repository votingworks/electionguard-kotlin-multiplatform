package electionguard.decrypt

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedTally
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.LagrangeCoordinate
import electionguard.ballot.decryptWithBetaToContestData
import electionguard.core.*
import electionguard.core.Base16.toHex
import mu.KotlinLogging

private val logger = KotlinLogging.logger("TallyDecryptor")

private const val verifySelections = true
private const val verifyContestData = true

/** Turn an EncryptedTally into a DecryptedTallyOrBallot. */
class TallyDecryptor(
    val group: GroupContext,
    val extendedBaseHash: UInt256,
    val publicKey: ElGamalPublicKey,
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
        contestId: String,
        contestDataDecryptions: ContestDataResults?, // results for this selection
    ): DecryptedTallyOrBallot.DecryptedContestData? {
        return contestDataDecryptions?.let {
            // v = Sum(v_i mod q); spec 2.0.0 eq (85)
            val response: ElementModQ = with(group) { contestDataDecryptions.responses.values.map { it }.addQ() }

            // finally we can create the proof
            if (contestDataDecryptions.collectiveChallenge == null) {
                logger.error { "$contestId: ContestDataResults missing challenge" }
                return null
            }
            val challenge = contestDataDecryptions.collectiveChallenge!!.toElementModQ(group)
            val proof = ChaumPedersenProof(challenge, response)

            if (contestDataDecryptions.beta == null) {
                logger.error { "$contestId: ContestDataResults missing beta" }
                return null
            }

            // use beta to do the decryption
            val contestData =
                contestDataDecryptions.ciphertext.decryptWithBetaToContestData(
                    publicKey,
                    extendedBaseHash,
                    contestId,
                    contestDataDecryptions.beta!!
                )
            if (contestData is Err) {
                return null
            }

            val decryptedContestData = DecryptedTallyOrBallot.DecryptedContestData(
                contestData.unwrap(),
                contestDataDecryptions.ciphertext,
                proof,
                contestDataDecryptions.beta!!,
            )

            if (verifyContestData) {
                val results = verifyContestData(contestId, decryptedContestData)
                if (results is Err) {
                    println(results)
                }
            }

            decryptedContestData
        }
    }

    // this is the verifier proof (box 11)
    private fun verifyContestData(where: String, decryptedContestData: DecryptedTallyOrBallot.DecryptedContestData): Result<Boolean, String> {
        val proof = decryptedContestData.proof
        val hashedCiphertext = decryptedContestData.encryptedContestData
        // a = g^v * K^c  (11.1,14.1)
        val a = group.gPowP(proof.r) * (publicKey powP proof.c)

        // b = C0^v * β^c (11.2,14.2)
        val b = (hashedCiphertext.c0 powP proof.r) * (decryptedContestData.beta powP proof.c)

        val results = mutableListOf<Result<Boolean, String>>()

        // (11.A,14.A) The given value v is in the set Zq.
        if (!proof.r.inBounds()) {
            results.add(Err("     (11.A,14.A) The value v is not in the set Zq.: '$where'"))
        }

        // (11.B) The challenge value c satisfies c = H(HE ; 0x31, K, C0 , C1 , C2 , a, b, β)
        val challenge = hashFunction(extendedBaseHash.bytes, 0x31.toByte(), publicKey.key,
            hashedCiphertext.c0,
            hashedCiphertext.c1.toHex(),
            hashedCiphertext.c2,
            a, b, decryptedContestData.beta)

        if (challenge != proof.c.toUInt256()) {
            results.add(Err("     (11.B,14.B) The challenge value is wrong: '$where'"))
        }
        return results.merge()
    }

    // TODO detect nulls
    private fun decryptSelection(
        selection: EncryptedTally.Selection,
        selectionDecryptions: DecryptionResults, // results for this selection
        contestId: String,
        stats: Stats,
    ): DecryptedTallyOrBallot.Selection {
        // v = Sum(v_i mod q); spec 2.0.0 eq 76
        val response: ElementModQ = with(group) { selectionDecryptions.responses.values.map { it }.addQ() }
        // finally we can create the proof
        val proof = ChaumPedersenProof(selectionDecryptions.collectiveChallenge!!.toElementModQ(group), response)
        val T = selection.encryptedVote.data / selectionDecryptions.M!! // "decrypted value" T = B · M −1, eq 64

        val decrypytedSelection = DecryptedTallyOrBallot.Selection(
            selection.selectionId,
            selectionDecryptions.tally!!,
            T,
            selection.encryptedVote,
            proof
        )

        if (verifySelections) {
            val startVerifyDetailed = getSystemTimeInMillis()
            if (!selectionDecryptions.checkIndividualResponses()) {
                println("checkIndividualResponses failed for  $contestId and ${selection.selectionId}")
            }
            stats.of("checkIndividualResponses", "selection", "selections")
                .accum(getSystemTimeInMillis() - startVerifyDetailed, 1)

            val startVerify = getSystemTimeInMillis()
            if (!decrypytedSelection.verifySelection()) {
                println("verifySelection failed for  $contestId and ${selection.selectionId}")
            }
            stats.of("verifySelection", "selection", "selections").accum(getSystemTimeInMillis() - startVerify, 1)
        }

        return decrypytedSelection
    }

    // this is the verifier proof (box 8)
    private fun DecryptedTallyOrBallot.Selection.verifySelection(): Boolean {
        return this.proof.validate2(publicKey.key, extendedBaseHash, this.bOverM, this.encryptedVote)
    }

    // Verify with spec 2.0.0 eq 75, 76
    private fun DecryptionResults.checkIndividualResponses(): Boolean {
        var ok = true
        for (partialDecryption in this.shares.values) {
            val guardianId = partialDecryption.guardianId
            val vi = this.responses[guardianId]
                ?: throw IllegalStateException("*** response not found for ${guardianId}")
            val wi = lagrangeCoordinates[guardianId]!!.lagrangeCoefficient
            val ci = wi * this.collectiveChallenge!!.toElementModQ(group) // spec 2.0.0 eq 72

            val inner = guardians.getGexpP(guardianId)
            val ap = group.gPowP(vi) * (inner powP ci) // eq 74
            if (partialDecryption.a != ap) {
                println(" ayes dont match for ${guardianId}")
                ok = false
            }

            val bp = (this.ciphertext.pad powP vi) * (partialDecryption.Mi powP ci) // eq 75
            if (partialDecryption.b != bp) {
                println(" bees dont match for ${guardianId}")
                ok = false
            }
        }
        return ok
    }
}