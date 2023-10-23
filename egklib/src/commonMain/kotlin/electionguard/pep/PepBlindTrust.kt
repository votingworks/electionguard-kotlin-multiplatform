package electionguard.pep

import com.github.michaelbull.result.*
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.EncryptedBallot
import electionguard.core.*
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.decrypt.DecryptorDoerre
import electionguard.decrypt.Guardians
import electionguard.input.ValidationMessages
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger("DistPep")

/** Egk PEP with blinding Guardians separate from decrypting Guardians, and a trusted admin. */
class PepBlindTrust(
    val group: GroupContext,
    val extendedBaseHash: UInt256,
    val jointPublicKey: ElGamalPublicKey,
    val guardians: Guardians, // all guardians
    val blindTrustees: List<PepTrusteeIF>, // the trustees used to blind the decryption
    decryptingTrustees: List<DecryptingTrusteeIF>, // the trustees available to decrypt
) : PepAlgorithm {
    val decryptor = DecryptorDoerre(group, extendedBaseHash, jointPublicKey, guardians, decryptingTrustees)
    val stats = Stats()

    // test if ballot1 and ballot2 are equivalent or not.
    override fun testEquivalent(ballot1: EncryptedBallot, ballot2: EncryptedBallot): Result<Boolean, String> {
        val result = doEgkPep(ballot1, ballot2)
        if (result is Err) {
            return Err(result.error)
        }
        val pep = result.unwrap()
        return Ok(pep.isEq)
    }

    override fun doEgkPep(ballot1: EncryptedBallot, ballot2: EncryptedBallot): Result<BallotPep, String> {
        val startPep = System.currentTimeMillis()

        // LOOK check ballotIds match, styleIds?
        val errorMesses = ValidationMessages("Ballot '${ballot1.ballotId}'", 1)
        if (ballot1.ballotId != ballot2.ballotId) {
            val message = "ballot ids not equal ${ballot1.ballotId} != ${ballot2.ballotId}"
            return Err(message)
        }

        // 	Enc(σj) = (αj, βj) for j ∈ {1, 2}.
        //	Let α = α1/α2 mod p, β = β1/β2 mod p
        // Create encryptedBallot consisting of the (α, β)
        val ratioBallot: EncryptedBallot = makeRatioBallot(ballot1, ballot2, errorMesses)
        if (errorMesses.hasErrors()) {
            val message = "${ballot1.ballotId} makeRatioBallot error $errorMesses"
            logger.warn { message }
            return Err(message)
        }

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
        val decryption: DecryptedTallyOrBallot = decryptor.decryptPep(ballotAB)

        //    (b) IsEq = (T == 1)
        //    (c) Send (IsEq, c, v, α, β, c′, v′, A, B, T) to V and publish to BB.
        workIterator = work23s.iterator()
        var isEq = true
        val contestsPEP =
            decryption.contests.map { dContest ->
                val selectionsPEP =
                    dContest.selections.map { dSelection ->
                        isEq = isEq && (dSelection.bOverM == group.ONE_MOD_P)
                        val work = workIterator.next()
                        SelectionPep(work, dSelection)
                    }
                ContestPep(dContest.contestId, selectionsPEP)
            }
        val ballotPEP = BallotPep(decryption.id, isEq, contestsPEP)

        stats.of("egkPep", "selection").accum(getSystemTimeInMillis() - startPep, ntexts)

        // step 6: verify
        val verifyResult = VerifierPep(group, extendedBaseHash, jointPublicKey).verify(ballotPEP)
        return if (verifyResult is Ok) Ok(ballotPEP) else Err(verifyResult.getError()!!)
    }

}
