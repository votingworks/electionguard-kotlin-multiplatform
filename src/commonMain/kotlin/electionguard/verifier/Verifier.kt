package electionguard.verifier

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.DecryptionResult
import electionguard.ballot.Guardian
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
import electionguard.publish.ElectionRecord
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

private val debugChannels = false
private val debugBallots = false

// quick proof verification - not necessarily the verification spec
class Verifier(val group: GroupContext, val electionRecord: ElectionRecord) {
    val jointPublicKey: ElGamalPublicKey
    val cryptoExtendedBaseHash: ElementModQ
    val decryption: DecryptionResult
    val guardians: List<Guardian>

    init {
        decryption = electionRecord.readDecryptionResult().getOrThrow { throw IllegalStateException(it) }
        jointPublicKey = decryption.tallyResult.jointPublicKey()
        cryptoExtendedBaseHash = decryption.tallyResult.cryptoExtendedBaseHash()
        guardians = decryption.tallyResult.electionIntialized.guardians
    }

    fun verify(): Boolean {
        val guardiansOk = verifyGuardianPublicKey()
        println(" verifyGuardianPublicKey= $guardiansOk\n")

        val ballotsOk = verifySubmittedBallots(electionRecord.iterateSubmittedBallots())
        println(" verifySubmittedBallots= $ballotsOk\n")

        val tallyOk = verifyDecryptedTally()
        println(" verifyDecryptedTally= $tallyOk\n")

        val allOk = guardiansOk && ballotsOk && tallyOk
        return allOk
    }

    fun verifyGuardianPublicKey(): Boolean {
        var allValid = true
        for (guardian in this.guardians) {
            var guardianOk = true
            guardian.coefficientProofs.forEachIndexed { index, proof ->
                val publicKey = ElGamalPublicKey(guardian.coefficientCommitments[index])
                val validProof = publicKey.hasValidSchnorrProof(proof)
                guardianOk = guardianOk && validProof
            }
            println(" Guardian ${guardian.guardianId} ok = ${guardianOk}")
            allValid = allValid && guardianOk
        }

        val jointPublicKeyComputed = this.guardians.map { it.publicKey() }.reduce { a, b -> a * b }
        allValid = allValid && jointPublicKey.equals(jointPublicKeyComputed)
        return allValid
    }

    fun verifyDecryptedTally(): Boolean {
        var allValid = true
        var ncontests = 0
        var nselections = 0
        var nshares = 0
        val tally = decryption.decryptedTally
        for (contest in tally.contests.values) {
            ncontests++

            for (selection in contest.selections.values) {
                nselections++
                val message = selection.message

                for (partialDecryption in selection.partialDecryptions) {
                    nshares++
                    val sproof: GenericChaumPedersenProof? = partialDecryption.proof
                    if (sproof != null) { // LOOK null id recovered
                        val guardian = this.guardians.find { it.guardianId.equals(partialDecryption.guardianId) }
                        val guardianKey = guardian?.publicKey()
                            ?: throw IllegalStateException("Cant find guardian ${partialDecryption.guardianId}")
                        val svalid = sproof.isValid(
                            group.G_MOD_P,
                            guardianKey,
                            message.pad,
                            partialDecryption.share(),
                            arrayOf(cryptoExtendedBaseHash, guardianKey, message.pad, message.data), // section 7
                            arrayOf(partialDecryption.share())
                        )
                        if (!svalid) {
                            println("Fail guardian $guardian share proof $sproof")
                        }
                        allValid = allValid && svalid
                    }
                }
            }
        }
        println("Tally '${tally.tallyId}' valid $allValid; ncontests = $ncontests; nselections = $nselections; nshares = $nshares")
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
        println(" VerifySubmittedBallots took $took millisecs for $count ballots = $perBallot msecs/ballot")
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
                this.jointPublicKey,
                this.cryptoExtendedBaseHash,
            )

            for (selection in contest.selections) {
                nselections++
                val sproof: DisjunctiveChaumPedersenProofKnownNonce = selection.proof
                val svalid = sproof.isValid(
                    selection.ciphertext,
                    this.jointPublicKey,
                    this.cryptoExtendedBaseHash,
                )
                cvalid = cvalid && svalid
            }
            // println("     Contest '${contest.contestId}' valid $cvalid")
            bvalid = bvalid && cvalid
        }
        if (debugBallots) println(" Ballot '${ballot.ballotId}' valid $bvalid; ncontests = $ncontests nselections = $nselections")
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
    verify: (SubmittedBallot) -> Boolean,
) = launch(Dispatchers.Default) {
    for (ballot in input) {
        if (debugChannels) println("$id channel working on ${ballot.ballotId}")
        allOk = allOk && verify(ballot)
        yield()
    }
}