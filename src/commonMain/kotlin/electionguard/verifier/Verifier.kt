package electionguard.verifier

import electionguard.ballot.ElectionContext
import electionguard.ballot.ElectionRecord
import electionguard.ballot.SubmittedBallot
import electionguard.core.ConstantChaumPedersenProofKnownNonce
import electionguard.core.DisjunctiveChaumPedersenProofKnownNonce
import electionguard.core.ElGamalCiphertext
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GenericChaumPedersenProof
import electionguard.core.GroupContext
import electionguard.core.encryptedSum
import electionguard.core.getSystemTimeInMillis
import electionguard.core.hasValidSchnorrProof
import electionguard.core.isValid
import electionguard.core.toElementModQ
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.math.roundToInt

// quick proof verification - not necessarily the verification spec
class Verifier(val group: GroupContext, val electionRecord: ElectionRecord) {
    val publicKey: ElGamalPublicKey
    val cryptoBaseHash: ElementModQ
    val context: ElectionContext

    init {
        if (electionRecord.context == null) {
            throw IllegalStateException("electionRecord.context is null")
        }
        publicKey = ElGamalPublicKey(electionRecord.context.jointPublicKey)
        cryptoBaseHash = electionRecord.context.cryptoExtendedBaseHash.toElementModQ(group)
        context = electionRecord.context
    }

    fun verifyGuardianPublicKey(): Boolean {
        if (electionRecord.guardianRecords == null) {
            return false
        }
        var allValid = true
        for (guardian in electionRecord.guardianRecords) {
            var guardianOk = true
            guardian.coefficientProofs.forEachIndexed { index, proof ->
                val publicKey = ElGamalPublicKey(guardian.coefficientCommitments[index])
                val validProof = publicKey.hasValidSchnorrProof(proof)
                guardianOk = guardianOk && validProof
            }
            println("Guardian ${guardian.guardianId} ok = ${guardianOk}")
            allValid = allValid && guardianOk
        }
        return allValid
    }

    fun verifyDecryptedTally(): Boolean {
        if (electionRecord.guardianRecords == null) {
            return false
        }
        if (electionRecord.decryptedTally == null) {
            return false
        }

        var allValid = true
        var ncontests = 0
        var nselections = 0
        var nshares = 0
        val tally = electionRecord.decryptedTally
        for (contest in tally.contests.values) {
            ncontests++

            for (selection in contest.selections.values) {
                nselections++
                val message = selection.message

                for (share in selection.shares) {
                    nshares++
                    val sproof: GenericChaumPedersenProof? = share.proof
                    if (sproof != null) {
                        val guardian = electionRecord.guardianRecords.find { it.guardianId.equals(share.guardianId) }
                        val guardianKey = guardian?.guardianPublicKey ?: group.G_MOD_P
                        val svalid = sproof.isValid(
                            group.G_MOD_P,
                            guardianKey,
                            message.pad,
                            share.share,
                            arrayOf(cryptoBaseHash, guardianKey, message.pad, message.data), // section 7
                            arrayOf(share.share)
                        )
                        if (!svalid) {
                            println("Fail guardian $guardian share proof $sproof")
                        }
                        allValid = allValid && svalid
                    }
                }
            }
        }
        println("\nTally '${tally.tallyId}' valid $allValid; ncontests = $ncontests; nselections = $nselections; nshares = $nshares\n")
        return allValid
    }

    // multithreaded version
    private val nthreads = 11
    fun verifySubmittedBallots(ballots: Iterable<SubmittedBallot>): Boolean {
        val starting = getSystemTimeInMillis()

        runBlocking {
            val verifierJobs = mutableListOf<Job>()
            val ballotProducer = produceBallots(ballots)
            repeat(nthreads) {
                verifierJobs.add(launchVerifier(it, ballotProducer) { ballot -> verifySubmittedBallot(ballot) })
            }

            // wait for all encryptions to be done, then close everything
            joinAll(*verifierJobs.toTypedArray())
        }

        val took = getSystemTimeInMillis() - starting
        val perBallot = (took.toDouble() / count).roundToInt()
        println("Took $took millisecs for $count ballots = $perBallot msecs/ballot")
        return allOk
    }

    fun verifySubmittedBallot(ballot: SubmittedBallot): Boolean {
        var bvalid = true
        var ncontests = 0
        var nselections = 0
        for (contest in ballot.contests) {
            ncontests++
            // recalculate ciphertextAccumulation
            val texts: List<ElGamalCiphertext> = contest.selections.map { it.ciphertext }
            val ciphertextAccumulation: ElGamalCiphertext = texts.encryptedSum()
            // test that the proof is correct
            val proof: ConstantChaumPedersenProofKnownNonce = contest.proof
            var cvalid = proof.isValid(
                ciphertextAccumulation,
                ElGamalPublicKey(context.jointPublicKey),
                context.cryptoExtendedBaseHash.toElementModQ(group),
            )

            for (selection in contest.selections) {
                nselections++
                val sproof: DisjunctiveChaumPedersenProofKnownNonce = selection.proof
                val svalid = sproof.isValid(
                    selection.ciphertext,
                    ElGamalPublicKey(context.jointPublicKey),
                    context.cryptoExtendedBaseHash.toElementModQ(group),
                )
                cvalid = cvalid && svalid
            }
            // println("     Contest '${contest.contestId}' valid $cvalid")
            bvalid = bvalid && cvalid
        }
        println("Ballot '${ballot.ballotId}' valid $bvalid; ncontests = $ncontests nselections = $nselections")
        return bvalid
    }
}

private var allOk = true
private var count = 0
fun CoroutineScope.produceBallots(producer: Iterable<SubmittedBallot>): ReceiveChannel<SubmittedBallot> = produce {
    for (ballot in producer) {
        send(ballot)
        yield()
        count++
    }
    channel.close()
}

// multiple encryptors allow parallelism at the ballot level
// LOOK not possible to do ballot chaining
fun CoroutineScope.launchVerifier(
    id: Int,
    input: ReceiveChannel<SubmittedBallot>,
    verify: (SubmittedBallot)-> Boolean,
) = launch(Dispatchers.Default) {
    for (ballot in input) {
        println("$id channel working on ${ballot.ballotId}")
        allOk = allOk && verify(ballot) // LOOK
        yield()
    }
    println("verify #$id done")
}