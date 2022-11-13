package electionguard.decrypt

import electionguard.ballot.LagrangeCoordinate
import electionguard.ballot.EncryptedTally
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.Guardian
import electionguard.core.*

// TODO Use a configuration to set to the maximum possible vote. Keep low for testing to detect bugs quickly.
private const val maxDlog: Int = 1000

/**
 * Orchestrates the decryption of encrypted Tallies and Ballots with DecryptingTrustees.
 * This is the only way that an EncryptedTally can be decrypted.
 * An EncryptedBallot can also be decrypted if you know the master nonce.
 */
class Decryptor(
    val group: GroupContext,
    val qbar: ElementModQ,
    val jointPublicKey: ElGamalPublicKey,
    guardians: List<Guardian>,
    private val decryptingTrustees: List<DecryptingTrusteeIF>, // the trustees available to decrypt
    val missingTrustees: List<String>, // ids of the missing trustees
) {
    val lagrangeCoordinates: Map<String, LagrangeCoordinate>
    val guardianMap = guardians.associateBy { it.guardianId }
    val stats = Stats()

    init {
        // build the lagrangeCoordinates, needed for output
        val dguardians = mutableListOf<LagrangeCoordinate>()
        for (trustee in decryptingTrustees) {
            val present: List<Int> = // available trustees minus me
                decryptingTrustees.filter { it.id() != trustee.id() }.map { it.xCoordinate() }
            val coeff: ElementModQ = group.computeLagrangeCoefficient(trustee.xCoordinate(), present)
            dguardians.add(LagrangeCoordinate(trustee.id(), trustee.xCoordinate(), coeff))
        }
        this.lagrangeCoordinates = dguardians.associate { it.guardianId to it }

        // configure the DecryptingTrustees
        for (trustee in decryptingTrustees) {
            val available = lagrangeCoordinates[trustee.id()]
                ?: throw RuntimeException("missing available ${trustee.id()}")
            trustee.setMissing(group, available.lagrangeCoefficient, missingTrustees)
        }
    }

    fun decryptBallot(ballot: EncryptedBallot): DecryptedTallyOrBallot {
        // pretend a ballot is a tally
        return ballot.convertToTally().decrypt(true)
    }

    fun EncryptedTally.decrypt(isBallot : Boolean = false): DecryptedTallyOrBallot {
        // we need the DecryptionResults from all trustees before we can do the challenges
        // LOOK we could parallelize this, really just for remote case
        val startDecrypt = getSystemTimeInMillis()
        val trusteeDecryptions = mutableListOf<TrusteeDecryptions>()
        for (decryptingTrustee in decryptingTrustees) {
            trusteeDecryptions.add( this.computeDecryptionShareForTrustee(decryptingTrustee, isBallot))
        }
        val decryptions = Decryptions()
        trusteeDecryptions.forEach { decryptions.addTrusteeDecryptions(it)}

        val ndecrypt = decryptions.shares.size + decryptions.contestData.size
        stats.of("computeShares").accum(getSystemTimeInMillis() - startDecrypt, ndecrypt)

        val startChallengeTotal = getSystemTimeInMillis()

        // compute the challenges for each DecryptionResults over all the guardians
        for ((id, results) in decryptions.shares) {
            // accumulate all of the shares calculated for the selection
            val shares = results.shares
            val Mbar: ElementModP = with(group) { shares.values.map { it.mbari }.multP() }
            // Calculate 𝑀 = 𝐵⁄(∏𝑀𝑖) mod 𝑝. (spec 1.52 section 3.5.2 eq 59)
            val M: ElementModP = results.ciphertext.data / Mbar
            // Now we know M, and since 𝑀 = K^t mod 𝑝, t = logK (M)
            results.dlogM = jointPublicKey.dLog(M, maxDlog) ?: throw RuntimeException("dlog failed on $id")
            results.M = M

            // collective proof (spec 1.52 section 3.5.3 eq 61)
            val a: ElementModP = with(group) { shares.values.map { it.a }.multP() }
            val b: ElementModP = with(group) { shares.values.map { it.b }.multP() }
            results.challenge = hashElements(qbar, jointPublicKey, results.ciphertext.pad, results.ciphertext.data, a, b, M) // eq 62
        }

        // compute the challenges for each ContestDataResult
        if (isBallot) {
            for (results in decryptions.contestData.values) {
                // accumulate all of the shares calculated for the selection, eq 69
                val shares = results.shares
                val beta: ElementModP = with(group) { shares.values.map { it.mbari }.multP() }
                results.beta = beta
                val a: ElementModP = with(group) { shares.values.map { it.a }.multP() }
                val b: ElementModP = with(group) { shares.values.map { it.b }.multP() }

                // collective challenge (spec 1.52 section 3.5.3 eq 70)
                results.challenge = hashElements(
                    qbar,
                    jointPublicKey,
                    results.ciphertext.c0,
                    // results.ciphertext.c1,
                    results.ciphertext.c2,
                    a,
                    b,
                    beta,
                ) // eq 62
            }
        }

        // send all challenges for both decryption and contestData, get results into the decryptions
        // LOOK we could parallelize this, really just for remote case
        val startChallengeCalls = getSystemTimeInMillis()
        val trusteeChallengeResponses = mutableListOf<TrusteeChallengeResponses>()
        for (trustee in decryptingTrustees) {
            trusteeChallengeResponses.add(decryptions.challengeTrustee(trustee))
        }
        trusteeChallengeResponses.forEach { cr ->
            cr.results.forEach {
                decryptions.addChallengeResponse(cr.id, it)
            }
        }

        stats.of("challengeCalls").accum(getSystemTimeInMillis() - startChallengeCalls, ndecrypt)
        stats.of("challengesTotal").accum(getSystemTimeInMillis() - startChallengeTotal, ndecrypt)

        val startTally = getSystemTimeInMillis()
        // After gathering the challenge responses from the available trustees, we can verify and publish.
        val decryptor = TallyDecryptor(group, qbar, jointPublicKey, lagrangeCoordinates, guardianMap, missingTrustees)
        val result = decryptor.decryptTally(this, decryptions, stats)

        stats.of("decryptTally").accum(getSystemTimeInMillis() - startTally, ndecrypt)

        return result
    }

    /**
     * Compute one guardian's share of a decryption, aka a 'partial decryption', for all selections of
     *  this tally/ballot.
     * @param trustee: The trustee who will partially decrypt the tally
     * @param isBallot: if a ballot or a tally, ie if it may have contest data to decrypt.
     * @return results for this trustee
     */
    private fun EncryptedTally.computeDecryptionShareForTrustee(
        trustee: DecryptingTrusteeIF,
        isBallot: Boolean,
    ) : TrusteeDecryptions {

        // Get all the text that need to be decrypted in one call, including from ContestData
        val texts: MutableList<ElementModP> = mutableListOf()
        for (contest in this.contests) {
            if (isBallot && contest.contestData != null) {
                texts.add(contest.contestData.c0)
            }
            for (selection in contest.selections) {
                texts.add(selection.ciphertext.pad)
            }
        }

        // decrypt all of them at once
        val results: List<PartialDecryption> = trustee.decrypt(group, texts, null)

        // Place the results into the TrusteeDecryptions
        val trusteeDecryptions = TrusteeDecryptions(trustee.id())
        var count = 0
        for (contest in this.contests) {
            if (isBallot && contest.contestData != null) {
                trusteeDecryptions.addContestDataResults(contest.contestId, contest.contestData, results[count++])
            }
            for (selection in contest.selections) {
                trusteeDecryptions.addDecryption(contest.contestId, selection.selectionId, selection.ciphertext, results[count++])
            }
        }
        return trusteeDecryptions
    }

    // challenges for one trustee
    fun Decryptions.challengeTrustee(
        trustee: DecryptingTrusteeIF,
    ) : TrusteeChallengeResponses {
        // Get all the challenges from the shares for this trustee
        val requests: MutableList<ChallengeRequest> = mutableListOf()
        for ((id, results) in this.shares) {
            val result = results.shares[trustee.id()] ?: throw IllegalStateException("missing ${trustee.id()}")
            requests.add(ChallengeRequest(id, results.challenge!!.toElementModQ(group), result.u))
        }
        // Get all the challenges from the contestData
        for ((id, results) in this.contestData) {
            val result = results.shares[trustee.id()] ?: throw IllegalStateException("missing ${trustee.id()}")
            requests.add(ChallengeRequest(id, results.challenge!!.toElementModQ(group), result.u))
        }

        // ask for all of them at once
        val results: List<ChallengeResponse> = trustee.challenge(group, requests)
        return TrusteeChallengeResponses(trustee.id(), results)
    }
}

/** Compute the lagrange coefficient, now that we know which guardians are present. spec 1.52 section 3.5.2, eq 55. */
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
        val selections = contest.selections.map {
            EncryptedTally.Selection(
                it.selectionId,
                it.sequenceOrder,
                it.selectionHash,
                it.ciphertext)
        }
        EncryptedTally.Contest(
            contest.contestId,
            contest.sequenceOrder,
            contest.contestHash,
            selections,
            contest.contestData,
        )
    }
    return EncryptedTally(this.ballotId, contests)
}