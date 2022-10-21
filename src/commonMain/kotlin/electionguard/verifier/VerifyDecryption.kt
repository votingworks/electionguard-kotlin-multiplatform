package electionguard.verifier

import com.github.michaelbull.result.*
import electionguard.ballot.Manifest
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.core.*
import kotlin.collections.mutableSetOf
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

private const val debug = false

/** Box 8, 9, 11, 12, 13 */
@OptIn(ExperimentalCoroutinesApi::class)
class VerifyDecryption(
    val group: GroupContext,
    val manifest: Manifest,
    val jointPublicKey: ElGamalPublicKey,
    val qbar: ElementModQ,
) {

    fun verify(decrypted: DecryptedTallyOrBallot, isBallot: Boolean, showTime : Boolean = false): Stats {
        val starting = getSystemTimeInMillis()

        var ncontests = 0
        var nselections = 0
        val results = mutableListOf<Result<Boolean, String>>()
        val ballotSelectionSet = mutableSetOf<String>()

        for (contest in decrypted.contests.values) {
            ncontests++
            val where = "${decrypted.id}/${contest.contestId}"
            if (manifest.contestIdToLimit[contest.contestId] == null) {
                results.add(Err("    9.C,13C Ballot contains contest not in manifest: '$where' "))
                continue
            }

            var contestVotes = 0
            for (selection in contest.selections.values) {
                nselections++
                val here = "${contest.contestId}/${selection.selectionId}"
                val where2 = "$${decrypted.id}/$here"
                ballotSelectionSet.add(here)

                if (!manifest.contestAndSelectionSet.contains(here)) {
                    results.add(Err("    9.D,13D Ballot contains selection not in manifest: '$where2' "))
                    continue
                }

                if (!selection.proof.r.inBounds()) {
                    results.add(Err("    8.A,11.A response out of bounds: '$where2' "))
                }

                // LOOK should be proof.validate(), but current GenericChaumPedersen is too awkward.
                val Mbar: ElementModP = selection.message.data / selection.value
                val a = group.gPowP(selection.proof.r) * (jointPublicKey powP selection.proof.c) // 8.1
                val b = (selection.message.pad powP selection.proof.r) * (Mbar powP selection.proof.c) // 8.2
                val challenge = hashElements(qbar, jointPublicKey, selection.message.pad, selection.message.data, a, b, selection.value) // 8.B
                if (challenge.toElementModQ(group) != selection.proof.c) {
                    results.add(Err("    8.B,11.B Challenge does not match: '$where2' "))
                }

                // M = K^t mod p.
                val tallyQ = selection.tally.toElementModQ(group)
                if (selection.value != jointPublicKey powP tallyQ) {
                    results.add(Err("    9.B,12.B Tally Decryption M = K^t mod p failed: '$where2'"))
                }

                if (isBallot && (selection.tally !in (0..1))) {
                    results.add(Err("     13.A ballot vote ${selection.tally} must be a 0 or a 1: '$where2'"))
                }
                contestVotes += selection.tally
            }
            if (isBallot) {
                val limit = manifest.contestIdToLimit[contest.contestId]!!
                if (contestVotes !in (0..limit)) {
                    results.add(Err("     13.B sum of votes ${contestVotes} in contest must be le than $limit: '$where'"))
                }
            }
        }

        manifest.contestAndSelectionSet.forEach {
            if (!ballotSelectionSet.contains(it)) {
                results.add(Err("    9.E Manifest contains selection not in ballot: '$it' "))
            }
        }

        val took = getSystemTimeInMillis() - starting
        if (showTime) println("   verifyDecryptedTally took $took millisecs")

        return Stats(decrypted.id, results.merge(), ncontests, nselections)
    }

    fun verifySpoiledBallotTallies(
        ballots: Iterable<DecryptedTallyOrBallot>,
        nthreads: Int,
        showTime: Boolean = false
    ): StatsAccum {
        val starting = getSystemTimeInMillis()

        runBlocking {
            val verifierJobs = mutableListOf<Job>()
            val ballotProducer = produceTallies(ballots)
            repeat(nthreads) {
                verifierJobs.add(launchVerifier(it, ballotProducer) { ballot -> verify(ballot, isBallot = true) })
            }

            // wait for all encryptions to be done, then close everything
            joinAll(*verifierJobs.toTypedArray())
        }

        val took = getSystemTimeInMillis() - starting
        val perBallot = if (count == 0) 0 else (took.toDouble() / count).roundToInt()
        if (showTime) println("   verifySpoiledBallotTallies took $took millisecs for $count ballots = $perBallot msecs/ballot")
        return globalStat
    }

    private val globalStat = StatsAccum()
    private var count = 0
    private fun CoroutineScope.produceTallies(producer: Iterable<DecryptedTallyOrBallot>): ReceiveChannel<DecryptedTallyOrBallot> =
        produce {
            for (tally in producer) {
                send(tally)
                yield()
                count++
            }
            channel.close()
        }

    private val mutex = Mutex()

    private fun CoroutineScope.launchVerifier(
        id: Int,
        input: ReceiveChannel<DecryptedTallyOrBallot>,
        verify: (DecryptedTallyOrBallot) -> Stats,
    ) = launch(Dispatchers.Default) {
        for (tally in input) {
            if (debug) println("$id channel working on ${tally.id}")
            val stat = verify(tally)
            mutex.withLock {
                globalStat.add(stat)
            }
            yield()
        }
    }

}