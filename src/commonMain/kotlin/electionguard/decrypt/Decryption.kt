package electionguard.decrypt

import electionguard.ballot.LagrangeCoordinate
import electionguard.ballot.EncryptedTally
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.Guardian
import electionguard.core.ElGamalCiphertext
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.hashElements
import electionguard.core.toElementModQ

// TODO Use a configuration to set to the maximum possible vote. Keep low for testing to detect bugs quickly.
private const val maxDlog: Int = 1000
private var first = false

/**
 * Orchestrates the decryption of encrypted Tallies and Ballots with DecryptingTrustees.
 */
class Decryption(
    val group: GroupContext,
    val qbar: ElementModQ,
    val jointPublicKey: ElGamalPublicKey,
    val guardians: List<Guardian>,
    private val decryptingTrustees: List<DecryptingTrusteeIF>,
    private val missingTrustees: List<String>,
) {
    val lagrangeCoordinates: Map<String, LagrangeCoordinate> by lazy {
        val dguardians = mutableListOf<LagrangeCoordinate>()
        for (otherTrustee in decryptingTrustees) {
            val present: List<Int> =
                decryptingTrustees.filter { it.id() != otherTrustee.id() }.map { it.xCoordinate() }
            val coeff: ElementModQ = group.computeLagrangeCoefficient(otherTrustee.xCoordinate(), present)
            dguardians.add(LagrangeCoordinate(otherTrustee.id(), otherTrustee.xCoordinate(), coeff))
        }
        // sorted by guardianId, to match PartialDecryption.lagrangeInterpolation()
        dguardians.associate { it.guardianId to  it }
    }

    fun decryptBallot(ballot: EncryptedBallot): DecryptedTallyOrBallot {
        // pretend a ballot is a tally
        return ballot.convertToTally().decrypt()
    }

    fun EncryptedTally.decrypt(): DecryptedTallyOrBallot {
        println("missingTrustees = $missingTrustees")
        val trusteeDecryptions = TrusteeDecryptions()

        // LOOK could parallelize this? or is there shared state?
        for (decryptingTrustee in decryptingTrustees) {
            val available = lagrangeCoordinates[decryptingTrustee.id()] ?: throw RuntimeException("missingh available $decryptingTrustee.id()")
            this.computeDecryptionShareForTrustee(
                decryptingTrustee, available.lagrangeCoordinate, trusteeDecryptions)
        }

        // we need all the results before we can do the challenges
        for ((id, results) in trusteeDecryptions.shares) {
            // accumulate all of the shares calculated for the selection
            val shares = results.shares
            val Mbar: ElementModP = with(group) { shares.values.map { it.Mbari }.multP() }
            // Calculate 𝑀 = 𝐵⁄(∏𝑀𝑖) mod 𝑝. (spec 1.52 section 3.5.2 eq 59)
            val M: ElementModP = results.ciphertext.data / Mbar
            // Now we know M, and since 𝑀 = K^t mod 𝑝, t = logK (M)
            results.dlogM = jointPublicKey.dLog(M, maxDlog) ?: throw RuntimeException("dlog failed on $id")
            results.M = M

            // collective proof (spec 1.52 section 3.5.3 eq 61)
            val a: ElementModP = with(group) { shares.values.map { it.a }.multP() }
            val b: ElementModP = with(group) { shares.values.map { it.b }.multP() }

            if (first) {
                println("qbar = $qbar")
                println("jointPublicKey = $jointPublicKey")
                println("A = ${results.ciphertext.pad}")
                println("B = ${results.ciphertext.data}")
                println("a = $a")
                println("b = $b")
                println("M = ${M}")
                first = false
            }

            results.challenge = hashElements(qbar, jointPublicKey, results.ciphertext.pad, results.ciphertext.data, a, b, M) // eq 62
        }

        // LOOK could parallelize this? or is there shared state?
        for (decryptingTrustee in decryptingTrustees) {
            trusteeDecryptions.challengeTrustee(decryptingTrustee)
        }

        // After gathering the challenge responses from the available guardians, we can verify and publish.
        val decryptor = TallyDecryptor(group, qbar, jointPublicKey, lagrangeCoordinates, guardians)
        return decryptor.decryptTally(this, trusteeDecryptions)
    }

    /**
     * Compute a guardian's share of a decryption, aka a 'partial decryption', for all selections of
     *  this tally/ballot.
     * @param trustee: The trustee who will partially decrypt the tally
     * @param lagrangeCoordinate: The trustee's lagrange Coordinate
     * @return a DecryptionShare for this trustee
     */
    private fun EncryptedTally.computeDecryptionShareForTrustee(
        trustee: DecryptingTrusteeIF,
        lagrangeCoordinate: ElementModQ,
        trusteeDecryptions: TrusteeDecryptions,
    ): TrusteeDecryptions {

        // Get all the Ciphertext that need to be decrypted in one call
        val texts: MutableList<ElGamalCiphertext> = mutableListOf()
        for (tallyContest in this.contests) {
            for (selection in tallyContest.selections) {
                texts.add(selection.ciphertext)
            }
        }
        // decrypt all of them at once
        val results: List<PartialDecryption> =
            trustee.decrypt(group, lagrangeCoordinate, missingTrustees, texts, null)

        // Place the results into the TrusteeDecryptions
        var count = 0
        for (contest in this.contests) {
            for (selection in contest.selections) {
                trusteeDecryptions.addDecryption(contest.contestId, selection.selectionId, selection.ciphertext, results[count])
                count++
            }
        }
        return trusteeDecryptions
    }

    fun TrusteeDecryptions.challengeTrustee(
        trustee: DecryptingTrusteeIF,
    ) {
        // Get all the Ciphertext that need to be decrypted
        val requests: MutableList<ChallengeRequest> = mutableListOf()
        for ((id, results) in this.shares) {
            val result = results.shares[trustee.id()] ?: throw IllegalStateException("missing ${trustee.id()}")
            requests.add(ChallengeRequest(id, results.challenge!!.toElementModQ(group), result.u, result.tm))
        }

        // ask for all of them at once
        val results: List<ChallengeResponse> = trustee.challenge(group, requests)

        // Place the results into the TrusteeDecryptions
        results.forEach { this.addChallengeResponse(trustee.id(), it) }
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
        // remove placeholders
        val selections = contest.selections.map {
            EncryptedTally.Selection(it.selectionId, it.sequenceOrder, it.selectionHash, it.ciphertext)
        }
        EncryptedTally.Contest(contest.contestId, contest.sequenceOrder, contest.contestHash, selections)
    }
    return EncryptedTally(this.ballotId, contests)
}


