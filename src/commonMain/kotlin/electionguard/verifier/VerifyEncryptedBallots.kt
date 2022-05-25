package electionguard.verifier

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.getError
import electionguard.ballot.EncryptedBallot
import electionguard.core.ConstantChaumPedersenProofKnownNonce
import electionguard.core.DisjunctiveChaumPedersenProofKnownNonce
import electionguard.core.ElGamalCiphertext
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.encryptedSum
import electionguard.core.getSystemTimeInMillis
import electionguard.core.isValid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.math.roundToInt

private const val debugBallots = false

@OptIn(ExperimentalCoroutinesApi::class)
class VerifyEncryptedBallots(
    val jointPublicKey: ElGamalPublicKey,
    val cryptoExtendedBaseHash: ElementModQ,
    private val nthreads: Int,
) {

    // multithreaded version
    fun verifyEncryptedBallots(ballots: Iterable<EncryptedBallot>): StatsAccum {
        val starting = getSystemTimeInMillis()

        runBlocking {
            val verifierJobs = mutableListOf<Job>()
            val ballotProducer = produceBallots(ballots)
            repeat(nthreads) {
                verifierJobs.add(launchVerifier(it, ballotProducer) { ballot -> verifyEncryptedBallot(ballot) })
            }

            // wait for all encryptions to be done, then close everything
            joinAll(*verifierJobs.toTypedArray())
        }

        val took = getSystemTimeInMillis() - starting
        val perBallot = (took.toDouble() / count).roundToInt()
        println("VerifyEncryptedBallots with $nthreads threads ok=${accumStats.allOk} took $took millisecs for $count ballots = $perBallot msecs/ballot")
        return accumStats
    }

    fun verifyEncryptedBallot(ballot: EncryptedBallot): Stats {
        var ncontests = 0
        var nselections = 0
        val errors = mutableListOf<String>()

        for (contest in ballot.contests) {
            ncontests++
            // recalculate ciphertextAccumulation
            val texts: List<ElGamalCiphertext> = contest.selections.map { it.ciphertext }
            val ciphertextAccumulation: ElGamalCiphertext = texts.encryptedSum()

            // test that the proof is correct
            val proof: ConstantChaumPedersenProofKnownNonce = contest.proof
            val cvalid = proof.isValid(
                ciphertextAccumulation,
                this.jointPublicKey,
                this.cryptoExtendedBaseHash,
                // LOOK how come we dont know the contest limit here?
            )
            if (!cvalid) {
                errors.add("    5.B cpp failed for ${ballot.ballotId}/${contest.contestId}")
            }

            for (selection in contest.selections) {
                nselections++
                val sproof: DisjunctiveChaumPedersenProofKnownNonce = selection.proof
                val svalid = sproof.isValid(
                    selection.ciphertext,
                    this.jointPublicKey,
                    this.cryptoExtendedBaseHash,
                )
                if (svalid is Err) {
                    errors.add(svalid.getError()!!)
                }
            }
        }
        val bvalid = errors.isEmpty()
        if (debugBallots) println(" Ballot '${ballot.ballotId}' valid $bvalid; ncontests = $ncontests nselections = $nselections")
        return Stats(ballot.ballotId, bvalid, ncontests, nselections, errors.joinToString("\n"))
    }

    private val accumStats = StatsAccum()
    private var count = 0
    private fun CoroutineScope.produceBallots(producer: Iterable<EncryptedBallot>): ReceiveChannel<EncryptedBallot> = produce {
        for (ballot in producer) {
            send(ballot)
            yield()
            count++
        }
        channel.close()
    }

    private fun CoroutineScope.launchVerifier(
        id: Int,
        input: ReceiveChannel<EncryptedBallot>,
        verify: (EncryptedBallot) -> Stats,
    ) = launch(Dispatchers.Default) {
        for (ballot in input) {
            if (debugBallots) println("$id channel working on ${ballot.ballotId}")
            val stat = verify(ballot)
            accumStats.add(stat) // LOOK should be in a mutex
            yield()
        }
    }

}