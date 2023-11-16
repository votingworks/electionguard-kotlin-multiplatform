package electionguard.rave

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.ballot.EncryptedBallot
import electionguard.core.*
import electionguard.util.ErrorMessages
import electionguard.util.Stats

/** PepBlindTrust adapted for mixnet. */
class MixnetPepBlindTrust(
    val group: GroupContext,
    val extendedBaseHash: UInt256,
    val jointPublicKey: ElGamalPublicKey,
    val blindTrustees: List<PepTrusteeIF>, // the trustees used to blind the decryption
    val decryptor: CiphertextDecryptor, // TODO replace with DecryptorDoerr using List<Ciphertext> instead of ballot.
) {
    val stats = Stats()

    // test if ballot1 and ballot2 are equivalent (or not).
    fun testEquivalent(ballot1: EncryptedBallot, ballot2: MixnetBallot): Result<BallotPep, ErrorMessages> {
        val errs = ErrorMessages("MixnetPepBlindTrust Ballot '${ballot1.ballotId}'", 1)

        val pep = doMixnetPep(ballot1, ballot2, errs)
        return if (pep == null) Err(errs) else Ok(pep!!)
    }

    /**
     * Create proof that ballot1 and ballot2 are equivalent (or not).
     * The returned DecryptedTallyOrBallot is the ratio of the two ballots for each selection.
     * Each decrypted selection has T == 1 (or not) and a corresponding proof.
     * Note that the two encrypted ballots must have been decrypted by the same encryptor, using
     * the same parameters (extendedBaseHash, jointPublicKey, guardians, decryptingTrustees).
     */
    fun doMixnetPep(ballot1: EncryptedBallot, ballot2: MixnetBallot, errs: ErrorMessages): BallotPep? {
        val startPep = System.currentTimeMillis()

        // 	Enc(σj) = (αj, βj) for j ∈ {1, 2}.
        //	Let α = α1/α2 mod p, β = β1/β2 mod p
        // Create encryptedBallot consisting of the (α, β)
        val ratioBallot = makeRatioMixnetBallot(ballot1, ballot2)

        // create list of ciphertext
        val ratioCiphertexts: List<ElGamalCiphertext> =
            ratioBallot.contests.map { it.selections }.flatten().map { it.encryptedVote }
        val ntexts = ratioCiphertexts.size
        val nb = blindTrustees.size

        // 1. all BGj in {BGj}:  // 4
        //    (a) ξj ← Zq
        //    (b) Compute Aj = α^ξj mod p and Bj = β^ξj mod p.
        //    (c) uj ← Zq
        //    (d) Compute aj = α^uj mod p and bj = β^uj mod p
        //    (e) Send (Aj, Bj, aj, bj) to admin
        // step 1: for each trustee, a list of its responses
        val step1s: List<List<BlindResponse>> = blindTrustees.map { it.blind(ratioCiphertexts) }
        require(step1s.size == nb) // list(nd, texts)

        // for each ciphertext, a list of responses from all the trustees
        val blindResponses = MutableList<MutableList<BlindResponse>>(ntexts) { mutableListOf() }
        step1s.forEach { responses ->
            responses.forEachIndexed { idx, response ->
                blindResponses[idx].add(response) // TODO better
            }
        }
        require(blindResponses.size == ntexts) // list(texts, nd)

        // data class BlindResponse(
        //    val Aj : ElementModP,
        //    val Bj : ElementModP,
        //    val aj : ElementModP,
        //    val bj : ElementModP,
        //    val u: ElementModQ,  // opaque, just pass back when challenging
        //    val eps: ElementModQ,  // opaque, just pass back when challenging
        //)

        //2: admin:
        //    A = Prod_j(Aj)
        //    B = Prod_j(Bj)
        //    If (A == 1) or (B == 1), reject.
        //3. admin:
        //    a = Prod_j(aj)
        //    b = Prod_j(bj)
        //    c = H(cons0; {cons}, K, α, β, A, B, a, b)
        //    Send challenge c to each DGi
        val work23s: List<BlindWorking> = blindResponses.zip(ratioCiphertexts).map { (blindResponse, ratioCiphertext) ->
            val A = with(group) { blindResponse.map { it.bigAj }.multP() }
            val B = with(group) { blindResponse.map { it.bigBj }.multP() }
            val a = with(group) { blindResponse.map { it.aj }.multP() }
            val b = with(group) { blindResponse.map { it.bj }.multP() }
            val c = hashFunction(
                extendedBaseHash.bytes,
                0x42.toByte(), // LOOK
                jointPublicKey.key,
                ratioCiphertext.pad, ratioCiphertext.data,
                A, B, a, b,
            )
            BlindWorking(ratioCiphertext, A, B, a, b, c.toElementModQ(group))
        }
        require(work23s.size == ntexts) // list(texts)

        // data class Working (
        //    val ciphertext: ElGamalCiphertext,
        //    val bigA : ElementModP,
        //    val bigB : ElementModP,
        //    val a : ElementModP,
        //    val b : ElementModP,
        //    val c: ElementModQ,
        //    val blindChallenges: MutableList<BlindChallenge> = mutableListOf(),
        //    var blindResponses: List<BlindResponse>? = null
        //)

        // step 3: form all the challenges to each trustee, put them in working
        // while were at it , we also need to know the original BlindResponse, so put those in working
        blindResponses.zip(work23s).map { (responsesForTrustees: List<BlindResponse>, work23: BlindWorking) ->
            responsesForTrustees.forEach { br ->
                work23.blindChallenges.add(BlindChallenge(work23.c, br.eps, br.u))
            }
            work23.blindResponses = responsesForTrustees
        }

        //4. all BGj in {BGj}:
        //     respond to challenge with vj = uj − c * ξj
        //     send to admin

        // step 4: gather all challenges, get response from each trustee
        val step4: List<List<BlindChallengeResponse>> = blindTrustees.mapIndexed { idx, trustee ->
            val allChallengesForIthTrustee = work23s.map { it.blindChallenges[idx] }
            trustee.challenge(allChallengesForIthTrustee)
        }
        require(step4.size == nb) // list(nb, texts)

        // data class BlindChallengeResponse(
        //    val response: ElementModQ,
        //)

        // for each ciphertext, a list of responses from all the trustees
        val step4invert = MutableList<MutableList<BlindChallengeResponse>>(ntexts) { mutableListOf() }
        step4.forEach { responses ->
            responses.forEachIndexed { idx, response ->
                step4invert[idx].add(response) // TODO better
            }
        }
        require(step4invert.size == ntexts) // list(texts, nb)

        // 5.a admin:
        work23s.zip(step4invert).forEach { (work23, step4) ->
            //      v = Sum_dj(vj)
            work23.v = with(group) { step4.map { it.response }.addQ() }

            //      verify if ChaumPedersenProof(c, v).verify(cons0; {cons1, K}, α, β, A, B).   // 4
            val proof = ChaumPedersenProof(work23.c, work23.v!!)
            val verifya = proof.verify(
                extendedBaseHash,
                0x42.toByte(),
                jointPublicKey.key,
                work23.ciphertextRatio.pad, work23.ciphertextRatio.data,
                work23.bigA, work23.bigB,
            )
            //      If true, can skip 5.b
            if (!verifya) {
                work23s.zip(step4invert).forEach { (work23, step4) -> // workj23s list(texts)
                    work23.blindResponses!!.zip(step4).forEach { (br, bcr) ->
                        //    for each BGj, verify that aj = α^vj * Aj^c and bj = β^vj * Bj^c   // 4 * nb
                        val ajp = (work23.ciphertextRatio.pad powP bcr.response) * (br.bigAj powP work23.c)
                        val bjp = (work23.ciphertextRatio.data powP bcr.response) * (br.bigBj powP work23.c)
                        require(ajp == br.aj)
                        require(bjp == br.bj)
                    }
                }
            }
        }

        // create an EncryptedBallot with the ciphertexts = (A, B), this is what we decrypt
        var workIterator = work23s.iterator()
        val contestsAB =
            ballot1.contests.map { contest ->
                val selectionsAB =
                    contest.selections.map { selection ->
                        val work = workIterator.next()
                        work.ciphertextAB = ElGamalCiphertext(work.bigA, work.bigB)
                        selection.copy(encryptedVote = work.ciphertextAB!!)
                    }
                contest.copy(selections = selectionsAB)
            }
        val ballotAB = ballot1.copy(contests = contestsAB)

        //6. admin:
        //    (a) decrypt (A, B): (T, ChaumPedersenProof(c',v')) = EGDecrypt(A, B)    // 4+5*nd
        // val decryption: DecryptedTallyOrBallot? = decryptor.decryptPep(ballotAB, errs.nested("decryptPep"))
        //require(decryption != null)

        //    (b) IsEq = (T == 1)
        //    (c) Send (IsEq, c, v, α, β, c′, v′, A, B, T) to V and publish to BB.
        workIterator = work23s.iterator()
        var isEq = true
        val contestsPEP = ballotAB.contests.map { contest ->
            val selectionsPEP =
                contest.selections.map { selection ->
                    val pepWithProof = decryptor.decryptPepWithProof(selection.encryptedVote)
                    isEq = isEq && (pepWithProof.bOverM == group.ONE_MOD_P)
                    val work = workIterator.next()
                    SelectionPep(selection, work, pepWithProof)
                }
            ContestPep(contest.contestId, selectionsPEP)
        }
        val ballotPEP = BallotPep(ballotAB.ballotId, isEq, contestsPEP)

        // debug: verify the decyption prrofs
        var countOk = 0
        ballotPEP.contests.forEach {
            it.selections.forEach {
                val ok = it.decryptionProof.verifyDecryption(extendedBaseHash, jointPublicKey.key, it.ciphertextAB, it.T)
                if (!ok) {
                    println("FAIL")
                } else countOk++
            }
        }
        println("  Verify ok $countOk")

        stats.of("MixnetBlindTrustPep", "ciphertext").accum(getSystemTimeInMillis() - startPep, ntexts)

        // step 6: verify
        // step 6: verify
        VerifierPep(group, extendedBaseHash, jointPublicKey).verify(ballotPEP, errs)

        return ballotPEP
    }

}

//////////////////////////////////////////////////////////////////////////////

// make a ballot replacing all the selection ciphertexts with the ratio of ballot1/ballot2 selection ciphertexts
fun makeRatioMixnetBallot(
    ballot1: EncryptedBallot,
    ballot2: MixnetBallot,
): EncryptedBallot {
    val ratioContests = mutableListOf<EncryptedBallot.Contest>()
    var count = 0
    for (contest1 in ballot1.contests) {
        val ratioSelections = mutableListOf<EncryptedBallot.Selection>()
        for (selection1 in contest1.selections) {
            val ratio = makeCiphertextRatio(selection1.encryptedVote, ballot2.ciphertext[count++])
            ratioSelections.add(selection1.copy(encryptedVote = ratio))
        }
        ratioContests.add(contest1.copy(selections = ratioSelections))
    }
    return ballot1.copy(contests = ratioContests)
}

private fun makeCiphertextRatio(ciphertext1: ElGamalCiphertext, ciphertext2: ElGamalCiphertext): ElGamalCiphertext {
    // replace with ((α1/α2), (β1/β2)))
    val alpha = (ciphertext1.pad div ciphertext2.pad)
    val beta = (ciphertext1.data div ciphertext2.data)
    return ElGamalCiphertext(alpha, beta)
}