package electionguard.verifier

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrapError
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.Manifest
import electionguard.core.*
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
import kotlin.math.roundToInt

private const val debugBallots = false

// TODO redo this with 1.51 Verification box 4, 5, 6
@OptIn(ExperimentalCoroutinesApi::class)
class VerifyEncryptedBallots(
    val group: GroupContext,
    val manifest: Manifest,
    val jointPublicKey: ElGamalPublicKey,
    val cryptoExtendedBaseHash: ElementModQ,
    private val nthreads: Int,
) {
    val aggregator = SelectionAggregator()

    fun verify(ballots: Iterable<EncryptedBallot>, showTime: Boolean = false): StatsAccum {
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
                    ) { ballot -> verifyEncryptedBallot(ballot) })
            }

            // wait for all verifications to be done, then close everything
            joinAll(*verifierJobs.toTypedArray())
        }

        // check duplicate confirmation codes (6.B)
        // LOOK what about checking for duplicate ballot ids?
        val errors = mutableListOf<String>()
        val checkDuplicates = mutableMapOf<UInt256, String>()
        confirmationCodes.forEach {
            if (checkDuplicates[it.code] != null) {
                errors.add("    6.B. Duplicate tracking code for ballot ${it.ballotId} and ${checkDuplicates[it.code]}")
            }
            checkDuplicates[it.code] = it.ballotId
        }
        accumStats.add(errors)

        if (showTime) {
            val took = getSystemTimeInMillis() - starting
            val perBallot = if (count == 0) 0 else (took.toDouble() / count).roundToInt()
            println("   VerifyEncryptedBallots with $nthreads threads ok=${accumStats.allOk()} took $took millisecs for $count ballots = $perBallot msecs/ballot")
        }
        return accumStats
    }

    fun verifyEncryptedBallot(ballot: EncryptedBallot): Stats {
        var ncontests = 0
        var nselections = 0
        val errors = mutableListOf<String>()

        val test6 = verifyTrackingCode(ballot)
        if (test6 is Err) {
            errors.add(test6.unwrapError())
        }

        // TODO redo this with 1.51 Verification box 5
        for (contest in ballot.contests) {
            val where = "${ballot.ballotId}/${contest.contestId}"
            ncontests++
            nselections += contest.selections.size

            // calculate ciphertextAccumulation (A, B), note 5.B unneeded because we dont keep (A,B) in election record
            val texts: List<ElGamalCiphertext> = contest.selections.map { it.ciphertext }
            val ciphertextAccumulation: ElGamalCiphertext = texts.encryptedSum()

            // test that the proof is correct; covers 5.C, 5.D, 5.E
            val proof: RangeChaumPedersenProofKnownNonce = contest.proof
            val cvalid = proof.validate(
                ciphertextAccumulation,
                this.jointPublicKey,
                this.cryptoExtendedBaseHash,
                manifest.contestIdToLimit[contest.contestId]!!
            )
            if (cvalid is Err) {
                errors.add("    5. ConstantChaumPedersenProofKnownNonce failed for $where = ${cvalid.error} ")
            }

            /* val challenge = contest.proof.proof.c
            val response = contest.proof.proof.r
            val alphaProduct = ciphertextAccumulation.pad
            val betaProduct = ciphertextAccumulation.data
            val expandedProof = contest.proof.proof.expand()
            val a: ElementModP = expandedProof.a
            val b: ElementModP
            if (!check5F(challenge, response, expandedProof.a, alphaProduct)) {
                errors.add(" 5.F Contest ${contest.contestId} failure")
            }
            if (!check5G(challenge, response, expandedProof.b, betaProduct, limit)) {
                errors.add(" 5.F Contest ${contest.contestId} failure")
            } */

            val svalid = verifySelections(ballot.ballotId, contest)
            if (svalid is Err) {
                errors.add(svalid.error)
            }
        }
        val bvalid = errors.isEmpty()
        if (debugBallots) println(" Ballot '${ballot.ballotId}' valid $bvalid; ncontests = $ncontests nselections = $nselections")
        return Stats(ballot.ballotId, bvalid, ncontests, nselections, errors)
    }

    // TODO redo this with 1.51 Verification box 6
    private fun verifyTrackingCode(ballot: EncryptedBallot): Result<Boolean, String> {
        val errors = mutableListOf<Result<Boolean, String>>()

        val cryptoHashCalculated = hashElements(ballot.ballotId, manifest.cryptoHashUInt256(), ballot.contests) // B_i
        if (cryptoHashCalculated != ballot.cryptoHash) {
            errors.add(Err("    6. Test ballot.cryptoHash failed for ${ballot.ballotId} "))
        }

        val trackingCodeCalculated = hashElements(ballot.codeSeed, ballot.timestamp, ballot.cryptoHash)
        if (trackingCodeCalculated != ballot.code) {
            errors.add(Err("    6.A Test ballot.trackingCode failed for ${ballot.ballotId} "))
        }
        return errors.merge()
    }

    /*
     * 5.F check if equation g ^ V mod p = a * A ^ C mod p is satisfied,
     * where
     *  C =  the contest proof challenge
     *  V =  the contest proof response
     *  (a, b) =  the contest proof encryption commitment to L
     *  A = the accumulative product of all the alpha/pad values on all selections within a contest
     *
    private fun check5F(challenge: ElementModQ, response: ElementModQ, a: ElementModP, alphaProduct: ElementModP): Boolean {
        val left: ElementModP = group.gPowP(response)
        val right: ElementModP =  a * (alphaProduct powP challenge)
        return left.equals(right)
    }

     * 5.G check if equation g ^ (L * C) * (K ^ V) mod p = b * B ^ C mod p is satisfied
     * where
     *  C = the contest proof challenge
     *  V = the contest proof response
     *  K = election public key
     *  (a, b) =  the contest proof encryption commitment to L
     *  B = the accumulative product of all the beta/data values on all selections within a contest
     *  L = election limit
     *
    private fun check5G(challenge: ElementModQ, response: ElementModQ, b: ElementModP, betaProduct: ElementModP, limit: Int): Boolean {
        val limitAsQ: ElementModQ = limit.toElementModQ(this.group)
        val leftTerm1: ElementModP = group.gPowP(limitAsQ * challenge)
        val leftTerm2: ElementModP = this.jointPublicKey powP response
        val left: ElementModP = leftTerm1.times(leftTerm2)
        val right: ElementModP =  b * (betaProduct powP challenge)
        return left.equals(right)
    }
    */

    // TODO redo this with 1.51 Verification box 4
    private fun verifySelections(ballotId: String, contest: EncryptedBallot.Contest): Result<Boolean, String> {
        val errors = mutableListOf<Result<Boolean, String>>()
        for (selection in contest.selections) {
            val where = "${ballotId}/${contest.contestId}/${selection.selectionId}"

            // test that the proof is correct covers 4.A, 4.B, 4.C, 4.D
            val svalid = selection.proof.validate(
                selection.ciphertext,
                this.jointPublicKey,
                this.cryptoExtendedBaseHash,
                1,
            )
            if (svalid is Err) {
                errors.add(Err("    4. DisjunctiveChaumPedersenProofKnownNonce failed for $where/${selection.selectionId} = ${svalid.error} "))
            }
        }

        /* 5.A verify the placeholder numbers match the maximum votes allowed
        val limit = manifest.contestIdToLimit[contest.contestId]
        if (limit == null) {
            errors.add(Err(" 5. Contest ${contest.contestId} not in Manifest"))
        } else {
            if (limit != nplaceholders) {
                errors.add(Err(" 5.A Contest placeholder $nplaceholders != $limit vote limit for contest ${contest.contestId}"))
            }
        } */
        return errors.merge()
    }

    private val accumStats = StatsAccum()
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
        verify: (EncryptedBallot) -> Stats,
    ) = launch(Dispatchers.Default) {
        for (ballot in input) {
            if (debugBallots) println("$id channel working on ${ballot.ballotId}")
            val stat = verify(ballot)
            mutex.withLock {
                agg.add(ballot) // this slows down the ballot parallelism: nselections * (2 (modP multiplication))
                confirmationCodes.add(ConfirmationCode(ballot.ballotId, ballot.code))
                accumStats.add(stat)
            }
            yield()
        }
    }
}

// check confirmation codes
data class ConfirmationCode(val ballotId: String, val code: UInt256)