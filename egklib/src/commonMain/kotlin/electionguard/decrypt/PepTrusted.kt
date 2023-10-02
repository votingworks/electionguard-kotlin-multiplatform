package electionguard.decrypt

import com.github.michaelbull.result.*
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.EncryptedBallot
import electionguard.core.*
import electionguard.input.ValidationMessages
import mu.KotlinLogging

private val logger = KotlinLogging.logger("DistPep")
private var first = false

// "Distributed Plaintext Equivalence Proof", distributed with trusted admin.
class PepTrusted(
    val group: GroupContext,
    val extendedBaseHash: UInt256,
    val jointPublicKey: ElGamalPublicKey,
    val guardians: Guardians, // all guardians
    decryptingTrustees: List<DecryptingTrusteeIF>, // the trustees available to decrypt
    val nag : Int, // number of admin guardians
) {
    val decryptor = DecryptorDoerre(group, extendedBaseHash, jointPublicKey, guardians, decryptingTrustees)

    // test if ballot1 and ballot2 are equivalent (or not).
    fun testEquivalent(ballot1: EncryptedBallot, ballot2: EncryptedBallot): Result<Boolean, String> {
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
    fun doEgkPep(ballot1: EncryptedBallot, ballot2: EncryptedBallot): Result<BallotPEP, String> {
        // LOOK check ballotIds match, styleIds?
        val errorMesses = ValidationMessages("Ballot '${ballot1.ballotId}'", 1)

        // 	Enc(σj) = (αj, βj) for j ∈ {1, 2}.
        //	Let α = α1/α2 mod p, β = β1/β2 mod p
        // Create encryptedBallot consisting of the (α, β)
        val ratioBallot = makeRatioBallot(ballot1, ballot2, errorMesses)
        if (errorMesses.hasErrors()) {
            val message = "${ballot1.ballotId} makeRatioBallot failed because $errorMesses"
            logger.atWarn().log(message)
            return Err(message)
        }

        // step 1: initialize guardians
        val admins = mutableListOf<AdminGuardian>()
        repeat(nag) { admins.add(AdminGuardian(group, it, ratioBallot)) }

        // create a working ballot to store intermediate results in
        val contestsW =
            ratioBallot.contests.map { contest ->
                val selectionsW =
                    contest.selections.map { selection ->
                        var aprod: ElementModP = group.ONE_MOD_P
                        var bprod: ElementModP = group.ONE_MOD_P
                        var Aprod: ElementModP = group.ONE_MOD_P
                        var Bprod: ElementModP = group.ONE_MOD_P
                        admins.forEach { admin ->
                            val sel = admin.selectionMap["${contest.contestId}#${selection.selectionId}"]!!
                            aprod *= sel.a
                            bprod *= sel.b
                            Aprod *= sel.ciphertextAB.pad
                            Bprod *= sel.ciphertextAB.data
                        }
                        SelectionWorking(
                            selection.selectionId,
                            selection.encryptedVote, // ciphertextRatio
                            ElGamalCiphertext(Aprod, Bprod),
                            aprod, bprod,
                        )
                    }
                ContestWorking(contest.contestId, selectionsW)
            }
        val ballotWorking = BallotWorking(ratioBallot.ballotId, contestsW)

        // create an EncryptedBallot with the ciphertexts = (A, B)
        val contestsAB =
            ratioBallot.contests.zip(ballotWorking.contests).map { (contest, contestW) ->
                val selectionsAB =
                    contest.selections.zip(contestW.selections).map { (selection, selectionW) ->
                        selection.copy(encryptedVote = selectionW.ciphertextAB)
                    }
                contest.copy(selections = selectionsAB)
            }
        val ballotAB = ratioBallot.copy(contests = contestsAB)

        // step 2. Use standard algorithm to decrypt ciphertextAB, and get the decryption proof:
        //    (T, ChaumPedersenProof(c',v')) = EGDecrypt(A, B)
        val decryption : DecryptedTallyOrBallot = decryptor.decryptPep(ballotAB)

        // step 3,4,5 compute challenge and get guardian responses and verify them
        ballotWorking.contests.map { contest ->
            val selectionsW =
                contest.selections.map { selection ->
                    // collective challenge
                    val c = hashFunction(
                        extendedBaseHash.bytes,
                        0x42, // LOOK
                        jointPublicKey.key,
                        selection.ciphertextRatio.pad, selection.ciphertextRatio.data,
                        selection.ciphertextAB.pad, selection.ciphertextAB.data,
                        selection.a,
                        selection.b,
                    ).toElementModQ(group)

                    var vSum = group.ZERO_MOD_Q
                    admins.forEach { admin ->
                        val selectionKey = "${contest.contestId}#${selection.selectionId}"
                        val sel = admin.selectionMap[selectionKey]!!
                        val vi = sel.challenge(c)
                        vSum = vSum + vi

                        //     (5.a) for each guardian, verify that aj = α^vj * Aj^cj and bj = β^vj * Bj^c for any j
                        val ratio = sel.ciphertextRatio
                        val AB = sel.ciphertextAB
                        val verifya = (ratio.pad powP vi) * (AB.pad powP c)
                        if ((verifya != sel.a)) {
                            println(" selection ${selectionKey} verifya failed for guardian ${admin.idx}")
                            errorMesses.add("verifya failed on '$selectionKey' for guardian ${admin.idx}")
                        }
                        val verifyb = (ratio.data powP vi) * (AB.data powP c)
                        if ((verifyb != sel.b)) {
                            println(" selection ${selectionKey} verifyb failed for guardian ${admin.idx}")
                            errorMesses.add("verifyb failed on '$selectionKey' for guardian ${admin.idx}")
                        }

                        /*  5.a for each guardian:
                        //   verify if ChaumPedersenProof(cj, vj).verify(cons0; {cons1, K}, α, β, Aj, Bj), otherwise, reject. [step 1.j in org]
                        val verify5a = ChaumPedersenProof(c, vi).verify(
                                extendedBaseHash,
                                0x42.toByte(),
                                jointPublicKey.key,
                                sel.ciphertextRatio.pad,sel. ciphertextRatio.data,
                                sel.ciphertextAB.pad, sel.ciphertextAB.data,
                            )
                        if (!verify5a) {
                            println(" selection ${selectionKey} verify5a = $verify5a for guardian ${admin.idx}")
                            // errorMesses.add("verify5a failed on '$selectionKey' for guardian ${admin.idx}")
                        }
                         */
                    }
                    selection.c = c
                    selection.v = vSum
                }
        }

        // 5. admin:
        //   (b) IsEq = (T == 1)
        //        Send (IsEq, c, v, α, β, c′, v′, A, B, T) to BB
        //        Output IsEq

        // the ballotWorking and the DecryptedBallot are zipped together to give the BallotPEP,
        // and sent to the BB and the verifier
        var isEq = true
        val contestsPEP =
            decryption.contests.zip(ballotWorking.contests).map { (dContest, contest) ->
                val selectionsPEP =
                    dContest.selections.zip(contest.selections).map { (dSelection, selection) ->
                        isEq = isEq && (dSelection.bOverM == group.ONE_MOD_P)
                        SelectionPEP(selection, dSelection)
                    }
                ContestPEP(dContest.contestId, selectionsPEP)
            }
        val ballotPEP = BallotPEP(isEq, decryption.id, contestsPEP)

        // step 6: verify
        val verifyResult = Verifier(group, extendedBaseHash, jointPublicKey).verifyPEP(ballotPEP)
        return if (verifyResult is Ok) Ok(ballotPEP) else Err(verifyResult.getError()!!)
    }

    data class BallotWorking(
        val ballotId: String,
        val contests: List<ContestWorking>,
    )

    data class ContestWorking(
        val contestId: String,
        val selections: List<SelectionWorking>,
    )

    inner class SelectionWorking(
        val selectionId: String,
        val ciphertextRatio: ElGamalCiphertext,
        val ciphertextAB: ElGamalCiphertext,
        val a: ElementModP,
        val b: ElementModP
    ) {
        var c: ElementModQ = group.ZERO_MOD_Q
        var v: ElementModQ = group.ZERO_MOD_Q
    }
}

class Verifier(
    val group: GroupContext,
    val extendedBaseHash: UInt256,
    val jointPublicKey: ElGamalPublicKey,
    ) {

    //    (a) verify if ChaumPedersenProof(c, v).verify(cons0; {cons1, K}, α, β, A, B). Otherwise, output “reject”.
    //
    //    (b) verify if ChaumPedersenProof(c', v').verifyDecryption(g, K, A, B, T) is true. Otherwise, output “reject”.
    //
    //    (c) If T = 1, IsEq = 1 and (A, B) ̸= (1, 1), output “alokkkkkkkkkkkkkij7uo8uccept(equal)”.
    //        If T ̸= 1, IsEq = 0, output “accept(unequal)”.
    //        Otherwise, output “reject”.

    fun verifyPEP(ballotPEP: BallotPEP): Result<Boolean, String> {
        val errors = mutableListOf<String>()
        ballotPEP.contests.forEach { contest ->
            contest.selections.forEach { pep ->
                val selectionKey = "${contest.contestId}#${pep.selectionId}"
                val verifya = ChaumPedersenProof(pep.c, pep.v).verify(
                        extendedBaseHash,
                        0x42.toByte(),
                        jointPublicKey.key,
                        pep.ciphertextRatio.pad, pep.ciphertextRatio.data,
                        pep.ciphertextAB.pad, pep.ciphertextAB.data,
                    )

                // if (!verifya) errors.add("PEP test 3.a failed on ${selectionKey}")
                val verifyb = ChaumPedersenProof(pep.c_prime, pep.v_prime).verifyDecryption(
                        extendedBaseHash,
                        jointPublicKey.key,
                        pep.ciphertextAB,
                        pep.T,
                    )
                if (!verifyb) errors.add("PEP test 3.b failed on ${contest.contestId}#${pep.selectionId}")
                println(" selection ${selectionKey} verifya = $verifya verifyb = $verifyb")
            }
        }
        return if (errors.isEmpty()) Ok(true) else Err(errors.joinToString(";"))
    }
}

class AdminGuardian(val group: GroupContext, val idx: Int, ratioBallot : EncryptedBallot) {
    private val workingValues: BallotValues
    val selectionMap = mutableMapOf<String, SelectionValues>()

    init {
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
                        val selection = SelectionValues(
                            ratioSelection.selectionId,
                            ratioSelection.encryptedVote,
                            ElGamalCiphertext(A, B),
                            eps, u, a, b
                        )
                        this.selectionMap["${contest.contestId}#${ratioSelection.selectionId}"] = selection
                        selection
                    }
                ContestValues(contest.contestId, selectionsAB)
            }
        this.workingValues = BallotValues(ratioBallot.ballotId, contestsAB)
    }

    data class BallotValues(
        val ballotId: String,
        val contests: List<ContestValues>,
    )

    data class ContestValues(
        val contestId: String,
        val selections: List<SelectionValues>,
    )

    data class SelectionValues(
        val selectionId: String,
        val ciphertextRatio: ElGamalCiphertext, // alpha, beta
        val ciphertextAB: ElGamalCiphertext, // A, B
        private val eps: ElementModQ,
        private val u: ElementModQ,
        val a: ElementModP,
        val b: ElementModP,
    ) {
        fun challenge(c : ElementModQ) : ElementModQ {
            return u - c * eps
        }
    }
}