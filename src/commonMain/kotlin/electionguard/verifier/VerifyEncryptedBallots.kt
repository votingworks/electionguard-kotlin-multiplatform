package electionguard.verifier

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.math.roundToInt

private const val debugBallots = false

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
        println(" verifyEncryptedBallots ok=${accumStats.allOk} took $took millisecs for $count ballots = $perBallot msecs/ballot")
        return accumStats
    }

    private fun verifyEncryptedBallot(ballot: EncryptedBallot): Stats {
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
        return Stats(bvalid, ncontests, nselections)
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