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

/** Box 8, 9, 10, 11, 12, 13 */
@OptIn(ExperimentalCoroutinesApi::class)
class VerifyDecryption(
    val group: GroupContext,
    val manifest: Manifest,
    val jointPublicKey: ElGamalPublicKey,
    val qbar: ElementModQ,
) {

    fun verify(decrypted: DecryptedTallyOrBallot, isBallot: Boolean, stats: Stats): Result<Boolean, String> {
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

            if (contest.decryptedContestData != null) {
                verifyContestData(where, contest.decryptedContestData)
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

        stats.of("verifyDecryption", "selections").accum(getSystemTimeInMillis() - starting, nselections)
        return results.merge()
    }

    private fun verifyContestData(where: String, contestData: DecryptedTallyOrBallot.DecryptedContestData): Result<Boolean, String> {
        val proof = contestData.proof
        val ciphertext = contestData.encryptedContestData
        // a = g^v * K^c (10.1)
        val a = group.gPowP(proof.r) * (jointPublicKey powP proof.c)

        // b = C0^v * β^c (10.2) b = C
        val beta = contestData.beta
        val b = (ciphertext.c0 powP proof.r) * (beta powP proof.c)

        val results = mutableListOf<Result<Boolean, String>>()

        if (!proof.r.inBounds()) {
            results.add(Err("     (10.A) The value v is not in the set Zq.: '$where'"))
        }
        //An election verifier must then confirm the following.
        //(10.A) The given value v is in the set Zq.
        //(10.B) The challenge value c satisfies c = H(Q, K, C0, C1, C2, a, b, β).
        val challenge = hashElements(
            qbar,
            jointPublicKey,
            ciphertext.c0,
            // ciphertext.c1,
            ciphertext.c2,
            a,
            b,
            beta,
        ) // eq 62
        if (challenge != proof.c.toUInt256()) {
            results.add(Err("     (10.B) The challenge value is wrong: '$where'"))
        }
        return results.merge()
    }

    ////////////////////////////////////////////////////////////////////////////////
    // coroutines

    fun verifySpoiledBallotTallies(
        ballots: Iterable<DecryptedTallyOrBallot>,
        nthreads: Int,
        stats: Stats,
        showTime: Boolean,
    ): Result<Boolean, String> {
        val starting = getSystemTimeInMillis()

        runBlocking {
            val verifierJobs = mutableListOf<Job>()
            val ballotProducer = produceTallies(ballots)
            repeat(nthreads) {
                verifierJobs.add(launchVerifier(it, ballotProducer) { ballot -> verify(ballot, isBallot = true, stats) })
            }

            // wait for all encryptions to be done, then close everything
            joinAll(*verifierJobs.toTypedArray())
        }

        val took = getSystemTimeInMillis() - starting
        val perBallot = if (count == 0) 0 else (took.toDouble() / count).roundToInt()
        if (showTime) println("   verifySpoiledBallotTallies took $took millisecs for $count ballots = $perBallot msecs/ballot wallclock")
        return globalResults.merge()
    }

    private val globalResults = mutableListOf<Result<Boolean, String>>()
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
        verify: (DecryptedTallyOrBallot) -> Result<Boolean, String>,
    ) = launch(Dispatchers.Default) {
        for (tally in input) {
            if (debug) println("$id channel working on ${tally.id}")
            val stat = verify(tally)
            mutex.withLock {
                globalResults.add(stat)
            }
            yield()
        }
    }

}