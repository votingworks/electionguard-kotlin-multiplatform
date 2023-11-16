package electionguard.rave

import com.github.michaelbull.result.*
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.EncryptedBallot
import electionguard.core.*
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.decrypt.DecryptorDoerre
import electionguard.decrypt.Guardians
import electionguard.util.ErrorMessages
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger("DistPep")
private const val show = false

// "Distributed Plaintext Equivalence Proof", simple version.
class PepSimple(
    val group: GroupContext,
    val extendedBaseHash: UInt256,
    val jointPublicKey: ElGamalPublicKey,
    val guardians: Guardians, // all guardians
    decryptingTrustees: List<DecryptingTrusteeIF>, // the trustees available to decrypt
) : PepAlgorithm {
    val decryptor = DecryptorDoerre(group, extendedBaseHash, jointPublicKey, guardians, decryptingTrustees)

    // test if ballot1 and ballot2 are equivalent (or not).
    override fun testEquivalent(ballot1: EncryptedBallot, ballot2: EncryptedBallot): Result<Boolean, String> {
        val result = doEgkPep(ballot1, ballot2)
        if (result is Err) {
            return Err(result.error)
        }
        val pep = result.unwrap()
        return Ok(pep.isEq)
    }

    /**
     * Create proof that ballot1 and ballot2 are equivalent (or not).
     * The returned DecryptedTallyOrBallot is the ratio of the two ballots for each selection.
     * Each decrypted selection has T == 1 (or not) and a corresponding proof.
     * Note that the two encrypted ballots must have been decrypted by the same encryptor, using
     * the same parameters (extendedBaseHash, jointPublicKey, guardians, decryptingTrustees).
     */
    override fun doEgkPep(ballot1: EncryptedBallot, ballot2: EncryptedBallot): Result<BallotPep, String> {
        // LOOK check ballotIds match, styleIds?
        val ballotMesses = ErrorMessages("Ballot '${ballot1.ballotId}'", 1)

        // 	Enc(σj) = (αj, βj) for j ∈ {1, 2}.
        //	Let α = α1/α2 mod p, β = β1/β2 mod p
        // Create encryptedBallot consisting of the (α, β)
        val ratioBallot = makeRatioBallot(ballot1, ballot2, ballotMesses)
        if (ballotMesses.hasErrors()) {
            val message = "${ballot1.ballotId} makeRatioBallot error $ballotMesses"
            logger.warn { message }
            return Err(message)
        }

        // step 1
        val ballotStep1: BallotStep1 = doStep1(ratioBallot)

        // create an EncryptedBallot from the ciphertextAB
        val contestsAB =
            ratioBallot.contests.zip(ballotStep1.contests).map { (ratioContest, step1Contest) ->
                val selectionsAB =
                    ratioContest.selections.zip(step1Contest.selections).map { (ratioSelection, step1Selection) ->
                        // replace the ciphertext with the ciphertextAB. proofs not valid.
                        ratioSelection.copy(encryptedVote = step1Selection.ciphertextAB)
                    }
                ratioContest.copy(selections = selectionsAB)
            }
        val ballotAB = ratioBallot.copy(contests = contestsAB)

        // step 2. Use standard algorithm to decrypt ciphertextAB, and get the decryption proof:
        //    (T, ChaumPedersenProof(c',v')) = EGDecrypt(A, B)
        //group.showAndClearCountPowP()
        val decryption: DecryptedTallyOrBallot? = decryptor.decryptPep(ballotAB, ErrorMessages("PepSimple"))
        require(decryption != null)
        val n = decryptor.nguardians
        val nd = decryptor.ndGuardians
        val q = decryptor.quorum
        val nenc = 1
        // val expect = (8 * nd) * nenc + n * n * q // simple
        // println(" after decryptPep ${group.showAndClearCountPowP()} expect = $expect")


        // the BallotStep1 and the DecryptedBallot are zipped together to give the BallotPEP,
        // and sent to the BB and the verifier
        var isEq = true
        val contestsPEP =
            decryption.contests.zip(ballotStep1.contests).map { (dContest, step1Contest) ->
                val selectionsPEP =
                    dContest.selections.zip(step1Contest.selections).map { (dSelection, step1Selection) ->
                        isEq = isEq && (dSelection.bOverM == group.ONE_MOD_P)
                        SelectionPep(step1Selection, dSelection)
                    }
                ContestPep(dContest.contestId, selectionsPEP)
            }
        val ballotPEP = BallotPep(decryption.id, isEq, contestsPEP)

        // step 6: verify
        VerifierPep(group, extendedBaseHash, jointPublicKey).verify(ballotPEP, ballotMesses)

        return if (!ballotMesses.hasErrors()) Ok(ballotPEP) else Err(ballotMesses.toString())
    }

    fun doStep1(ratioBallot: EncryptedBallot): BallotStep1 {
        val contestsAB =
            ratioBallot.contests.map { contest ->
                val selectionsAB =
                    contest.selections.map { ratioSelection ->
                        val (alpha, beta) = ratioSelection.encryptedVote
                        // 1.a ξ ← Zq
                        // 1.b Compute A = α^ξ mod p and B = β^ξ mod p
                        val eps = group.randomElementModQ(minimum = 2)
                        val A = alpha powP eps
                        val B = beta powP eps

                        // 1.c u ← Zq
                        val u = group.randomElementModQ(minimum = 2)
                        // 1.d Compute a = α^u mod p and b = β^u mod p
                        val a = alpha powP u
                        val b = beta powP u
                        // 1.e c = H(cons0; cons1, K, α, β, A, B, a, b)
                        val c = hashFunction(
                            extendedBaseHash.bytes,
                            0x42.toByte(),
                            jointPublicKey.key,
                            alpha, beta, A, B, a, b
                        ).toElementModQ(group)
                        // 1.f v = u - cξ
                        val v = u - c * eps

                        if (show) {
                            println("doStep1")
                            println("  extendedBaseHash = ${extendedBaseHash.bytes.contentToString()}")
                            println("  separator = ${0x42}")
                            println("  jointPublicKey = $jointPublicKey")
                            println("  alpha = $alpha")
                            println("  beta = $beta")
                            println("  A = $A")
                            println("  B = $B")
                            println("  a = $a")
                            println("  b = $b")
                            println("  c = $c")
                            println("  v = $v")
                        }

                        SelectionStep1(
                            ratioSelection.selectionId,
                            ratioSelection.encryptedVote,
                            ElGamalCiphertext(A, B),
                            c,
                            v
                        )
                    }
                ContestStep1(contest.contestId, selectionsAB)
            }
        return BallotStep1(ratioBallot.ballotId, contestsAB)
    }

    data class BallotStep1(
        val ballotId: String,
        val contests: List<ContestStep1>,
    )

    data class ContestStep1(
        val contestId: String,
        val selections: List<SelectionStep1>,
    )

    data class SelectionStep1(
        val selectionId: String,
        val ciphertextRatio: ElGamalCiphertext,
        val ciphertextAB: ElGamalCiphertext,
        val c: ElementModQ, // 2.c
        val v: ElementModQ, // 2.d
    )
}

// make a ballot replacing all the selection ciphertexts with the ratio of ballot1/ballot2 selection ciphertexts
// also make sure that the two ballots have identical contests and selections
fun makeRatioBallot(
    ballot1: EncryptedBallot,
    ballot2: EncryptedBallot,
    ballotMesses: ErrorMessages
): EncryptedBallot {
    val ratioContests = mutableListOf<EncryptedBallot.Contest>()
    // also make sure that the two ballots have identical contests and selections
    val contest1Ids = ballot1.contests.associateBy { it.contestId }
    for (contest2 in ballot2.contests) {
        if (!contest1Ids.contains(contest2.contestId)) {
            ballotMesses.add("ballot1 missing contest id '${contest2.contestId}'")
        } else {
            ratioContests.add(makeRatioContest(contest1Ids[contest2.contestId]!!, contest2, ballotMesses))
        }
    }
    val contest2Ids = ballot2.contests.associateBy { it.contestId }
    for (contest1 in ballot1.contests) {
        if (!contest2Ids.contains(contest1.contestId)) {
            ballotMesses.add("ballot2 missing contest id '${contest1.contestId}'")
        }
    }
    return ballot1.copy(contests = ratioContests)
}

private fun makeRatioContest(
    contest1: EncryptedBallot.Contest,
    contest2: EncryptedBallot.Contest,
    ballotMesses: ErrorMessages
): EncryptedBallot.Contest {
    val contestMesses = ballotMesses.nested("Contest " + contest1.contestId)

    if (contest1.sequenceOrder != contest2.sequenceOrder) {
        val msg = "ballot1 contest '${contest1.contestId}' sequenceOrder ${contest1.sequenceOrder} " +
                " does not match manifest Ballot2 contest sequenceOrder ${contest2.sequenceOrder}"
        contestMesses.add(msg)
    }

    val ratioSelections = mutableListOf<EncryptedBallot.Selection>()
    val selection1Ids = contest1.selections.associateBy { it.selectionId }
    for (selection2 in contest2.selections) {
        if (!selection1Ids.contains(selection2.selectionId)) {
            ballotMesses.add("contest1 missing selection id '${selection2.selectionId}'")
        } else {
            ratioSelections.add(
                makeRatioSelection(
                    selection1Ids[selection2.selectionId]!!,
                    selection2,
                    contestMesses
                )
            )
        }
    }

    val selection2Ids = contest2.selections.associateBy { it.selectionId }
    for (selection1 in contest1.selections) {
        if (!selection2Ids.contains(selection1.selectionId)) {
            ballotMesses.add("contest2 missing selection id '${selection1.selectionId}'")
        }
    }
    // make a copy, replacing the selections. other fields no longer valid.
    return contest1.copy(selections = ratioSelections)
}

private fun makeRatioSelection(
    selection1: EncryptedBallot.Selection,
    selection2: EncryptedBallot.Selection,
    contestMesses: ErrorMessages
): EncryptedBallot.Selection {
    val selectionMesses = contestMesses.nested("Selection " + selection1.selectionId)

    if (selection1.sequenceOrder != selection2.sequenceOrder) {
        val msg = "ballot1 selection '${selection1.selectionId}' sequenceOrder ${selection1.sequenceOrder} " +
                " does not match ballot2 selection sequenceOrder ${selection2.sequenceOrder}"
        selectionMesses.add(msg)
    }

    val ciphertext1 = selection1.encryptedVote
    val ciphertext2 = selection2.encryptedVote
    // replace with ((α1/α2), (β1/β2)))
    val alpha = (ciphertext1.pad div ciphertext2.pad)
    val beta = (ciphertext1.data div ciphertext2.data)
    val ratio = ElGamalCiphertext(alpha, beta)
    // replace the ciphertext with the ratio. proofs not valid.
    return selection1.copy(encryptedVote = ratio)
}