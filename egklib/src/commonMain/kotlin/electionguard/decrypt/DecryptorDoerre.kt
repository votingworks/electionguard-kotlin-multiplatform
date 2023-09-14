package electionguard.decrypt

import electionguard.ballot.LagrangeCoordinate
import electionguard.ballot.EncryptedTally
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.core.*
import electionguard.core.Base16.toHex

// TODO Use a configuration to set to the maximum possible vote. Keep low for testing to detect bugs quickly.
private const val maxDlog: Int = 1000

/**
 * Orchestrates the decryption of encrypted Tallies and Ballots with DecryptingTrustees.
 * This is the only way that an EncryptedTally can be decrypted.
 * An EncryptedBallot can also be decrypted if you know the master nonce.
 */
class DecryptorDoerre(
    val group: GroupContext,
    val extendedBaseHash: UInt256,
    val jointPublicKey: ElGamalPublicKey,
    val guardians: Guardians, // all guardians
    private val decryptingTrustees: List<DecryptingTrusteeIF>, // the trustees available to decrypt
) {
    val lagrangeCoordinates: Map<String, LagrangeCoordinate>
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
        this.lagrangeCoordinates = dguardians.associateBy { it.guardianId }
    }

    fun decryptBallot(ballot: EncryptedBallot): DecryptedTallyOrBallot {
        // pretend a ballot is a tally
        return ballot.convertToTally().decrypt(true)
    }

    var first = false
    fun EncryptedTally.decrypt(isBallot : Boolean = false): DecryptedTallyOrBallot {
        // get the DecryptionResults from all trustees before we can do the challenges
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

        // compute M for each DecryptionResults over all the shares from available guardians
        for ((id, dresults) in decryptions.shares) {
            // lagrange weighted product of the shares, M = Prod(M_i^w_i) mod p; spec 2.0.0, eq 68
            val weightedProduct = with(group) {
                dresults.shares.map { (key, value) ->
                    val coeff = lagrangeCoordinates[key] ?: throw IllegalArgumentException()
                    value.Mi powP coeff.lagrangeCoefficient
                }.multP()
            }

            // T = B · M−1 mod p; spec 2.0.0, eq 64
            val T = dresults.ciphertext.data / weightedProduct
            // T = K^t mod p, take log to get t = tally
            dresults.tally = jointPublicKey.dLog(T, maxDlog) ?: throw RuntimeException("dlog failed on $id")
            dresults.M = weightedProduct

            // compute the collective challenge, needed for the collective proof; spec 2.0.0 eq 70
            val a: ElementModP = with(group) { dresults.shares.values.map { it.a }.multP() }
            val b: ElementModP = with(group) { dresults.shares.values.map { it.b }.multP() }
            // "collective challenge" c = H(HE ; 0x30, K, A, B, a, b, M ) ; spec 2.0.0 eq 71
            dresults.collectiveChallenge = hashFunction(extendedBaseHash.bytes, 0x30.toByte(), jointPublicKey.key, dresults.ciphertext.pad, dresults.ciphertext.data, a, b, weightedProduct)

            if (first) { // temp debug, when a,b dont validate
                println(" decrypt extendedBaseHash = $extendedBaseHash")
                println(" jointPublicKey = $jointPublicKey")
                println(" message.pad = ${dresults.ciphertext.pad}")
                println(" message.data = ${dresults.ciphertext.data}")
                println(" a= $a")
                println(" b= $b")
                println(" M = $weightedProduct")
                println(" c = ${dresults.collectiveChallenge}")
                first = false
            }
        }

        // note that the proof is only in the decrypted ballot, and is only checked by the verifier
        // that means you could get this wrong and encrypt/decrypt would still work, just proof validation would fail
        if (isBallot) {
            for (cresults in decryptions.contestData.values) {
                // lagrange weighted product of the shares, beta = Prod(M_i^w_i) mod p; spec 2.0.0, eq 78
                val weightedProduct = with(group) {
                    cresults.shares.map { (key, value) ->
                        val coeff = lagrangeCoordinates[key] ?: throw IllegalArgumentException()
                        value.Mi powP coeff.lagrangeCoefficient
                    }.multP()
                }
                cresults.beta = weightedProduct

                // compute the collective challenge, needed for the collective proof; spec 2.0.0 eq 80
                val a: ElementModP = with(group) { cresults.shares.values.map { it.a }.multP() }
                val b: ElementModP = with(group) { cresults.shares.values.map { it.b }.multP() }

                // "joint challenge" c = H(HE ; 0x31, K, C0 , C1 , C2 , a, b, β) ; 2.0, eq 81
                cresults.collectiveChallenge = hashFunction(extendedBaseHash.bytes, 0x31.toByte(), jointPublicKey.key,
                    cresults.ciphertext.c0,
                    cresults.ciphertext.c1.toHex(),
                    cresults.ciphertext.c2,
                    a, b, weightedProduct)
            }
        }

        // now that we have the collective challenges, gather the individual challenges for both decryption and
        // contestData from each trustee, to construct the proofs.
        val startChallengeCalls = getSystemTimeInMillis()
        val trusteeChallengeResponses = mutableListOf<TrusteeChallengeResponses>()
        for (trustee in decryptingTrustees) {
            trusteeChallengeResponses.add(decryptions.challengeTrustee(trustee))
        }
        trusteeChallengeResponses.forEach { challengeResponses ->
            challengeResponses.results.forEach {
                decryptions.addChallengeResponse(challengeResponses.id, it)
            }
        }

        stats.of("challengeCalls").accum(getSystemTimeInMillis() - startChallengeCalls, ndecrypt)
        stats.of("challengesTotal").accum(getSystemTimeInMillis() - startChallengeTotal, ndecrypt)
        val startTally = getSystemTimeInMillis()

        // After gathering the challenge responses from the available trustees, we can verify and publish.
        // Put that code into a different class to keep this one from getting too big.
        val tallyDecryptor = TallyDecryptor(group, extendedBaseHash, jointPublicKey, lagrangeCoordinates, guardians)
        val result = tallyDecryptor.decryptTally(this, decryptions, stats)

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
                texts.add(selection.encryptedVote.pad)
            }
        }

        // decrypt all of them at once
        val results: List<PartialDecryption> = trustee.decrypt(group, texts)

        // Place the results into the TrusteeDecryptions
        val trusteeDecryptions = TrusteeDecryptions(trustee.id())
        var count = 0
        for (contest in this.contests) {
            if (isBallot && contest.contestData != null) {
                trusteeDecryptions.addContestDataResults(contest.contestId, contest.contestData, results[count++])
            }
            for (selection in contest.selections) {
                trusteeDecryptions.addDecryption(contest.contestId, selection.selectionId, selection.encryptedVote, results[count++])
            }
        }
        return trusteeDecryptions
    }

    // challenges for one trustee
    fun Decryptions.challengeTrustee(
        trustee: DecryptingTrusteeIF,
    ) : TrusteeChallengeResponses {
        val wi = lagrangeCoordinates[trustee.id()]!!.lagrangeCoefficient
        // Get all the challenges from the shares for this trustee
        val requests: MutableList<ChallengeRequest> = mutableListOf()
        for ((id, results) in this.shares) {
            val result = results.shares[trustee.id()] ?: throw IllegalStateException("missing ${trustee.id()}")
            // spec 2.0.0, eq 72
            val ci = wi * results.collectiveChallenge!!.toElementModQ(group)
            requests.add(ChallengeRequest(id, ci, result.u))
        }
        // Get all the challenges from the contestData
        for ((id, results) in this.contestData) {
            val result = results.shares[trustee.id()] ?: throw IllegalStateException("missing ${trustee.id()}")
            // spec 2.0.0, eq 72
            val ci = wi * results.collectiveChallenge!!.toElementModQ(group)
            requests.add(ChallengeRequest(id, ci, result.u))
        }

        // ask for all of them at once
        val results: List<ChallengeResponse> = trustee.challenge(group, requests)
        return TrusteeChallengeResponses(trustee.id(), results)
    }
}

/** Compute the lagrange coefficient, now that we know which guardians are present; 2.0, section 3.6.2, eq 67. */
fun GroupContext.computeLagrangeCoefficient(coordinate: Int, present: List<Int>): ElementModQ {
    val others: List<Int> = present.filter { it != coordinate }
    if (others.isEmpty()) {
        return this.ONE_MOD_Q
    }
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
                it.encryptedVote)
        }
        EncryptedTally.Contest(
            contest.contestId,
            contest.sequenceOrder,
            selections,
            contest.contestData,
        )
    }
    return EncryptedTally(this.ballotId, contests, emptyList())
}