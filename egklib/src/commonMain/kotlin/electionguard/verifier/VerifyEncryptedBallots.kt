package electionguard.verifier

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionConfig
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.EncryptedBallotChain
import electionguard.ballot.ManifestIF
import electionguard.core.*
import electionguard.publish.ElectionRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield

private const val debugBallots = false

/** Box 4,5,6,16,17. Can be multithreaded. */
@OptIn(ExperimentalCoroutinesApi::class)
class VerifyEncryptedBallots(
    val group: GroupContext,
    val manifest: ManifestIF,
    val jointPublicKey: ElGamalPublicKey,
    val extendedBaseHash: UInt256, // He
    val config: ElectionConfig,
    private val nthreads: Int,
) {
    val aggregator = SelectionAggregator() // for box 7

    fun verifyBallots(ballots: Iterable<EncryptedBallot>, stats: Stats, showTime: Boolean = false): Result<Boolean, String> {
        val starting = getSystemTimeInMillis()

        runBlocking {
            val verifierJobs = mutableListOf<Job>()
            val ballotProducer = produceBallots(ballots)
            repeat(nthreads) {
                verifierJobs.add(
                    launchVerifier(
                        it,
                        ballotProducer,
                        aggregator
                    ) { ballot -> verifyEncryptedBallot(ballot, stats) })
            }

            // wait for all verifications to be done
            joinAll(*verifierJobs.toTypedArray())
        }

        // check duplicate confirmation codes (6.C): LOOK what if there are multiple records for the election?
        // LOOK what about checking for duplicate ballot ids?
        val checkDuplicates = mutableMapOf<UInt256, String>()
        confirmationCodes.forEach {
            if (checkDuplicates[it.code] != null) {
                allResults.add(Err("    6.C. Duplicate confirmation code for ballot ${it.ballotId} and ${checkDuplicates[it.code]}"))
            }
            checkDuplicates[it.code] = it.ballotId
        }

        if (showTime) {
            val took = getSystemTimeInMillis() - starting
            val perBallot = if (count == 0) 0 else (took.toDouble() / count).sigfig()
            println("   VerifyEncryptedBallots with $nthreads threads took $took millisecs wallclock for $count ballots = $perBallot msecs/ballot")
        }
        return allResults.merge()
    }

    fun verifyEncryptedBallot(ballot: EncryptedBallot, stats: Stats): Result<Boolean, String> {
        val starting = getSystemTimeInMillis()
        val results = mutableListOf<Result<Boolean, String>>()

        var ncontests = 0
        var nselections = 0
        for (contest in ballot.contests) {
            val where = "${ballot.ballotId}/${contest.contestId}"
            ncontests++
            nselections += contest.selections.size

            // Box 4 : selection limit
            contest.selections.forEach {
                results.add(verifySelection(where, it))
            }

            // Verification 5 (Adherence to vote limits)
            // For each contest on each cast ballot, an election verifier must compute the contest totals Q
            //  (5.1) ᾱ = Qi αi mod p,
            //  (5.2) β̄ = i βi mod p,
            // where the (αi , βi ) represent all possible selections for the contest, as well as the values
            //  (5.3) aj = g vj · ᾱcj mod p for all 0 ≤ j ≤ L,
            //  (5.4) bj = K wj · β̄ cj mod p, where wj = (vj − jcj ) mod q for all 0 ≤ j ≤ L,
            //  (5.5) c = H(HE ; 21, K, ᾱ, β̄, a0 , b0 , a1 , b1 , . . . , aL , bL ).
            // An election verifier must then confirm the following:
            //  (5.A) The given values αi and βi are each in Zrp .
            //  (5.B) The given values cj and vj are each in Zq for all 0 ≤ j ≤ L.
            //  (5.C) The equation c = (c0 + c1 + · · · + cL ) mod q is satisfied.
            val texts: List<ElGamalCiphertext> = contest.selections.map { it.encryptedVote }
            val ciphertextAccumulation: ElGamalCiphertext = texts.encryptedSum()
            // test that the proof is correct covers 5.A, 5.B, 5.C
            val cvalid = contest.proof.validate2(
                ciphertextAccumulation,
                this.jointPublicKey,
                this.extendedBaseHash,
                manifest.contestLimit(contest.contestId)
            )
            if (cvalid is Err) {
                results.add(Err("    5. ChaumPedersenProof failed for $where = ${cvalid.error} "))
            }

            // 6A : contest hash
            // χl = H(HE ; 23, Λl , K, α1 , β1 , α2 , β2 . . . , αm , βm ). (58)
            val ciphers = mutableListOf<ElementModP>()
            texts.forEach {
                ciphers.add(it.pad)
                ciphers.add(it.data)
            }
            val contestHash = hashFunction(extendedBaseHash.bytes, 0x23.toByte(), contest.contestId, jointPublicKey.key, ciphers)
            if (contestHash != contest.contestHash) {
                results.add(Err("    6.A. Incorrect contest hash for contest ${contest.contestId} "))
            }

            if (ballot.isPreencrypt) {
                results.add(verifyPreencryption(ballot.ballotId, contest))
            }
        }

        if (!ballot.isPreencrypt) {
            // (6.B) The ballot confirmation code H(B) has been correctly computed from the contest hashes and Baux as
            //   H(B) = H(HE ; 24, χ1 , χ2 , . . . , χmB , Baux ).  (59)
            val contestHashes = ballot.contests.map { it.contestHash }
            val confirmationCode = hashFunction(extendedBaseHash.bytes, 0x24.toByte(), contestHashes, ballot.codeBaux)
            if (confirmationCode != ballot.confirmationCode) {
                results.add(Err("    6.B. Incorrect ballot confirmation code for ballot ${ballot.ballotId} "))
            }
        } else {
            results.add(verifyPreencryptedCode(ballot))
        }

        stats.of("verifyEncryptions", "selection").accum(getSystemTimeInMillis() - starting, nselections)
        if (debugBallots) println(" Ballot '${ballot.ballotId}' ncontests = $ncontests nselections = $nselections")
        return results.merge()
    }

    // Verification 4 (Correctness of selection encryptions)
    // For each possible selection on each cast ballot, an election verifier must compute the values
    //  (4.3) aj = g vj · αcj mod p for all 0 ≤ j ≤ L,
    //  (4.4) bj = K wj · β cj mod p, where wj = (vj − jcj ) mod q for all 0 ≤ j ≤ L,
    //  (4.5) c = H(HE ; 21, K, α, β, a0 , b0 , a1 , b1 , . . . , aL , bL ),
    // where L is the selection limit. An election verifier must then confirm the following:
    //  (4.A) The given values α and β are in the set Zrp . A value x is in Zrp if and only if x is an integer
    //       such that 0 ≤ x < p and xq mod p = 1.
    //  (4.B) The given values cj and vj are each in the set Zq for all 0 ≤ j ≤ L. A value x is in Zq if and only
    //       if x is an integer such that 0 ≤ x < q.
    //  (4.C) The equation c = (c0 + c1 + · · · + cL ) mod q is satisfied.
    private fun verifySelection(where: String, selection: EncryptedBallot.Selection): Result<Boolean, String> {
        val errors = mutableListOf<Result<Boolean, String>>()
        val here = "${where}/${selection.selectionId}"

        // test that the proof is correct covers 4.A, 4.B, 4.C
        val svalid = selection.proof.validate2(
            selection.encryptedVote,
            this.jointPublicKey,
            this.extendedBaseHash,
            1,
        )
        if (svalid is Err) {
            errors.add(Err("    4. DisjunctiveChaumPedersenProofKnownNonce failed for ${here}} = ${svalid.error} "))
        }
        return errors.merge()
    }

    //////////////////////////////////////////////////////////////////////////////
    // ballot chaining

    fun verifyConfirmationChain(consumer: ElectionRecord): Result<Boolean, String> {
        val results = mutableListOf<Result<Boolean, String>>()

        consumer.encryptingDevices().forEach { device ->
            // println("verifyConfirmationChain device=$device")
            val ballotChainResult = consumer.readEncryptedBallotChain(device)
            if (ballotChainResult is Err) {
                results.add(ballotChainResult)
            } else {
                val ballotChain: EncryptedBallotChain = ballotChainResult.unwrap()
                val ballots = consumer.encryptedBallots(device) { true }

                // (6.D) The initial hash code H0 satisfies H0 = H(HE ; 24, Baux,0 )
                // and Baux,0 contains the unique voting device information as specified in the election manifest file. TODO how?
                val H0 = hashFunction(extendedBaseHash.bytes, 0x24.toByte(), config.configBaux0).bytes // eq 60

                // (6.E) For all 1 ≤ j ≤ ℓ, the additional input byte array used to compute Hj = H(Bj ) is equal to Baux = H(Bj−1) ∥ Baux,0 .
                var prevCC = H0
                var first = true
                ballots.forEach { ballot ->
                    val expectedBaux = if (first) H0 else hashFunction(prevCC, config.configBaux0).bytes // eq 61
                    first = false
                    if (!expectedBaux.contentEquals(ballot.codeBaux)) {
                        results.add(Err("    6.E. additional input byte array Baux != H(Bj−1 ) ∥ Baux,0 for ballot=${ballot.ballotId}"))
                    }
                    prevCC = ballot.confirmationCode.bytes
                }
                // (6.F) The final additional input byte array is equal to Baux = H(Bℓ ) ∥ Baux,0 ∥ b("CLOSE") where
                //        H(Bℓ ) is the final confirmation code on this device.
                val bauxFinal = hashFunction(prevCC, config.configBaux0, "CLOSE")
                // (6.G) The closing hash is correctly computed as H = H(HE ; 24, Baux ).
                val expectedClosingHash = hashFunction(extendedBaseHash.bytes, 0x24.toByte(), bauxFinal)
                if (expectedClosingHash != ballotChain.closingHash) {
                    results.add(Err("    6.G. The closing hash is not equal to H = H(HE ; 24, bauxFinal ) for encrypting device=$device"))
                }
            }
        }
        return results.merge()
    }

    //////////////////////////////////////////////////////////////////////////////
    // pre-encryption

    // TODO specify sigma in manifest
    private fun sigma(hash : UInt256) : String = hash.toHex().substring(0, 5)

    /*
    For every pre-encrypted cast ballot contained in the election record:
    • The ballot confirmation code correctly matches the hash of all contest hashes on the ballot (listed sequentially).
    • Each contest hash correctly matches the hash of all selection hashes (including null selection
      hashes) within that contest (sorted within each contest).
    • All short codes shown to voters are correctly computed from selection hashes in the election record which are,
      in turn, correctly computed from the pre-encryption vectors published in the election record.
    • For contests with selection limit greater than 1, the selection vectors published in the election
      record match the product of the pre-encryptions associated with the short codes listed as selected.

    For every pre-encrypted ballot listed in the election record as uncast:
    • The ballot confirmation code correctly matches the hash of all contest hashes on the ballot (listed sequentially).
    • Each contest hash correctly matches the hash of all selection hashes (including null selection
      hashes) within that contest (sorted within each contest).
    • All short codes on the ballot are correctly computed from the selection hashes in the election
      record which are, in turn, correctly computed from the pre-encryption vectors published in the election record.
    • The decryptions of all pre-encryptions correspond to the plaintext values indicated in the
      election manifest.
     */

    // Box 14 == Box 4, Box 15 == Box 5

    // Verification 16 (Validation of short codes in pre-encrypted ballots)
    // An election verifier must confirm for every short code ω in the election record that ω = Ω(ψ) where
    // ψ is the associated selection hash.
    // For cast ballots, this includes all short codes that are published in the election record whose associated
    // selection hashes correspond to selection vectors that are accumulated to form tallies.
    // TODO For spoiled ballots, this includes all selection vectors on the ballot.
    private fun verifyPreencryption(ballotId: String, contest: EncryptedBallot.Contest): Result<Boolean, String> {
        val results = mutableListOf<Result<Boolean, String>>()

        if (contest.preEncryption == null) {
            results.add(Err("    17. Contest ${contest.contestId} for preencrypted '${ballotId}' has no preEncryption"))
            return results.merge()
        }
        val cv = contest.preEncryption
        val contestLimit = manifest.contestLimit(contest.contestId)
        val nselection = contest.selections.size

        require(contestLimit == cv.selectedVectors.size)
        require(contestLimit + nselection == cv.allSelectionHashes.size)

        // All short codes on the ballot are correctly computed from the pre-encrypted selections associated with each short code
        cv.selectedVectors.forEach { sv ->
            if (sv.shortCode != sigma(sv.selectionHash)) {
                results.add(Err("    16. Contest ${contest.contestId} shortCode '${sv.shortCode}' has no match"))
            }
        }

        // Note that in a contest with a selection limit of one, the selection vector will be identical to one of
        // the pre-encryption selection vectors. However, when a contest has a selection limit greater than
        // one, the resulting selection vector will be a product of multiple pre-encryption selection vectors.

        val selectionVector : List<ElGamalCiphertext> = contest.selections.map { it.encryptedVote }
        require (contestLimit == cv.selectedVectors.size)

        // product of multiple pre-encryption selection vectors. component-wise I think
        for (idx in 0 until nselection) {
            val compList = cv.selectedVectors.map { it.encryptions[idx] }
            val sum = compList.encryptedSum()
            if (sum != selectionVector[idx]) {
                results.add(Err("    16. Contest ${contest.contestId} (contestLimit=$contestLimit) selectionVector $idx does not match product"))
            }
        }

        return results.merge()
    }

    // Verification 17 (Validation of confirmation codes in pre-encrypted ballots)
    // An election verifier must confirm the following for each pre-encrypted ballot B:
    // (17.A) For each selection in each contest on the ballot and the corresponding selection vector
    //    Ψi,m = ⟨E1 , E2 , . . . , Em ⟩ consisting of the selection encryptions Ej = (αj , βj ), the selection
    //    hash ψi satisfies ψi = H(HE ; 40, λi , K, α1 , β1 , α2 , β2 , . . . , αm , βm ).
    // (17.B) The contest hash χl for the l-th contest on the ballot for all 1 ≤ l ≤ mB has been correctly
    //    computed from the selection hashes ψi as
    //     χl = H(HE ; 41, Λl , K, ψσ(1) , ψσ(2) , . . . , ψσ(m+L) ),
    //     where σ is a permutation and ψσ(1) < ψσ(2) < · · · < ψσ(m+L) .
    // (17.C) The ballot confirmation code H(B) has been correctly computed from the (sequentially ordered) contest
    //     hashes and if specified in the election manifest file from the additional byte array Baux as
    //     H(B) = H(HE ; 42, χ1 , χ2 , . . . , χmB , Baux ).
    //
    // (17.D) There are no duplicate confirmation codes, i.e. among the set of submitted (cast and challenged) ballots,
    //     no two have the same confirmation code.
    //
    // TODO Additionally, if the election manifest file specifies a hash chain, an election verifier must confirm the
    //  following for each pre-encrypted ballot generation device:
    // (17.E) The initial hash code H0 satisfies H0 = H(HE ; 42, Baux,0 ) and Baux,0 contains the unique
    //     device information for the device generating the pre-encrypted ballots as specified in the
    //     election manifest file.
    // (17.F) For all 1 ≤ i ≤ ℓ, the additional input byte array used to compute Hi = H(Bi ) is equal to
    //     Baux = H(Bi−1 ) ∥ Baux,0 .
    // (17.G) The final additional input byte array is equal to Baux = H(Bℓ ) ∥ Baux,0 ∥ b("CLOSE") and
    //     H(Bℓ ) is the final confirmation code on this device.
    // (17.H) The closing hash is correctly computed as H = H(HE ; 42, Baux ).
    private fun verifyPreencryptedCode(ballot: EncryptedBallot): Result<Boolean, String> {
        val errors = mutableListOf<String>()

        val contestHashes = mutableListOf<UInt256>()
        for (contest in ballot.contests) {
            if (contest.preEncryption == null) {
                errors.add("    17. Contest ${contest.contestId} for preencrypted '${ballot.ballotId}' has no preEncryption")
                continue
            }
            val cv = contest.preEncryption
            for (sv in cv.selectedVectors) {
                val hashVector: List<ElementModP> = sv.encryptions.map{ listOf(it.pad, it.data) }.flatten()
                val selectionHash = hashFunction(extendedBaseHash.bytes, 0x40.toByte(), jointPublicKey.key, hashVector)
                if (selectionHash != sv.selectionHash) {
                    errors.add("    17.A. Incorrect selectionHash for selection shortCode=${sv.shortCode} contest=${contest.contestId} ballot='${ballot.ballotId}' ")
                }
            }

            // χl = H(HE ; 41, Λl , K, ψσ(1) , ψσ(2) , . . . , ψσ(m+L) )
            val preencryptionHash = hashFunction(extendedBaseHash.bytes, 0x41.toByte(), contest.contestId, jointPublicKey.key, cv.allSelectionHashes)
            if (preencryptionHash != cv.preencryptionHash) {
                errors.add("    17.B. Incorrect contestHash for ${contest.contestId} ballot='${ballot.ballotId}' ")
            }
            contestHashes.add(preencryptionHash)
        }

        val confirmationCode = hashFunction(extendedBaseHash.bytes, 0x42.toByte(), contestHashes, ballot.codeBaux)
        if (confirmationCode != ballot.confirmationCode) {
            errors.add("    17.C. Incorrect confirmationCode ballot='${ballot.ballotId}' ")
        }

        return if (errors.isEmpty()) Ok(true) else Err(errors.joinToString("\n"))
    }

    //////////////////////////////////////////////////////////////
    // coroutines
    private val allResults = mutableListOf<Result<Boolean, String>>()
    private var count = 0
    private fun CoroutineScope.produceBallots(producer: Iterable<EncryptedBallot>): ReceiveChannel<EncryptedBallot> =
        produce {
            for (ballot in producer) {
                send(ballot)
                yield()
                count++
            }
            channel.close()
        }

    private val confirmationCodes = mutableListOf<ConfirmationCode>()
    private val mutex = Mutex()

    private fun CoroutineScope.launchVerifier(
        id: Int,
        input: ReceiveChannel<EncryptedBallot>,
        agg: SelectionAggregator,
        verify: (EncryptedBallot) -> Result<Boolean, String>,
    ) = launch(Dispatchers.Default) {
        for (ballot in input) {
            if (debugBallots) println("$id channel working on ${ballot.ballotId}")
            val result = verify(ballot)
            mutex.withLock {
                agg.add(ballot) // this slows down the ballot parallelism: nselections * (2 (modP multiplication))
                confirmationCodes.add(ConfirmationCode(ballot.ballotId, ballot.confirmationCode))
                allResults.add(result)
            }
            yield()
        }
    }
}

// check confirmation codes
private data class ConfirmationCode(val ballotId: String, val code: UInt256)