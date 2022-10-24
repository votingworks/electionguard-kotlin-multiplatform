package electionguard.decrypt

import electionguard.ballot.LagrangeCoordinate
import electionguard.ballot.EncryptedTally
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.Guardian
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.hashElements
import electionguard.core.toElementModQ

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
    val guardians: List<Guardian>,
    private val decryptingTrustees: List<DecryptingTrusteeIF>, // the trustees available to decrypt
    missingTrustees: List<String>, // ids of the missing trustees
) {
    val lagrangeCoordinates: Map<String, LagrangeCoordinate>

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
        for (decryptingTrustee in decryptingTrustees) {
            val available = lagrangeCoordinates[decryptingTrustee.id()]
                ?: throw RuntimeException("missing available $decryptingTrustee.id()")
            decryptingTrustee.setMissing(group, available.lagrangeCoefficient, missingTrustees)
        }
    }

    fun decryptBallot(ballot: EncryptedBallot): DecryptedTallyOrBallot {
        // pretend a ballot is a tally
        return ballot.convertToTally().decrypt()
    }

    fun EncryptedTally.decrypt(): DecryptedTallyOrBallot {
        val trusteeDecryptions = TrusteeDecryptions()

        for (decryptingTrustee in decryptingTrustees) {
            this.computeDecryptionShareForTrustee(decryptingTrustee, trusteeDecryptions)
        }

        // we need all the results before we can do the challenges
        for ((id, results) in trusteeDecryptions.shares) {
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

        for (decryptingTrustee in decryptingTrustees) {
            trusteeDecryptions.challengeTrustee(decryptingTrustee)
        }

        // After gathering the challenge responses from the available trustees, we can verify and publish.
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
        trusteeDecryptions: TrusteeDecryptions,
    ): TrusteeDecryptions {

        // Get all the text that need to be decrypted in one call
        val texts: MutableList<ElementModP> = mutableListOf()
        for (contest in this.contests) {
            for (selection in contest.selections) {
                texts.add(selection.ciphertext.pad)
            }
        }

        // decrypt all of them at once
        val results: List<PartialDecryption> = trustee.decrypt(group, texts, null)

        // Place the results into the TrusteeDecryptions
        var count = 0
        for (contest in this.contests) {
            for (selection in contest.selections) {
                trusteeDecryptions.addDecryption(contest.contestId, selection.selectionId, selection.ciphertext, results[count++])
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
            requests.add(ChallengeRequest(id, results.challenge!!.toElementModQ(group), result.u))
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