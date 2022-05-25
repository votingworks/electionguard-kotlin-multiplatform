package electionguard.verifier

import electionguard.ballot.Guardian
import electionguard.ballot.PlaintextTally
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
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
class VerifyDecryptedTally(
    val group: GroupContext,
    val jointPublicKey: ElGamalPublicKey,
    val cryptoExtendedBaseHash: ElementModQ,
    val guardians: List<Guardian>,
) {

    fun verifyDecryptedTally(tally: PlaintextTally): Stats {
        var ncontests = 0
        var nselections = 0
        var nshares = 0
        val errors = mutableListOf<String>()

        for (contest in tally.contests.values) {
            ncontests++

            for (selection in contest.selections.values) {
                nselections++
                val message = selection.message

                for (partialDecryption in selection.partialDecryptions) {
                    nshares++
                    if (partialDecryption.proof != null) {
                        val guardian = this.guardians.find { it.guardianId == partialDecryption.guardianId }
                        val guardianKey = guardian?.publicKey()
                        if (guardianKey != null) {
                            val svalid = partialDecryption.proof.isValid(
                                group.G_MOD_P,
                                guardianKey,
                                message.pad,
                                partialDecryption.share(),
                                arrayOf(cryptoExtendedBaseHash, guardianKey, message.pad, message.data), // section 7
                                arrayOf(partialDecryption.share())
                            )
                            if (!svalid) {
                                errors.add("Fail guardian $guardian share proof ${partialDecryption.proof}")
                            }
                        } else {
                            errors.add("Cant find guardian ${partialDecryption.guardianId}")
                        }

                    } else if (partialDecryption.recoveredDecryptions.isNotEmpty()) {

                        for (recoveredDecryption in partialDecryption.recoveredDecryptions) {
                            val gx = recoveredDecryption.recoveryKey
                            val hx = recoveredDecryption.share
                            if (!recoveredDecryption.proof.isValid(
                                    group.G_MOD_P,
                                    gx,
                                    message.pad,
                                    hx,
                                    arrayOf(cryptoExtendedBaseHash, gx, message.pad, message.data), // section 7
                                    arrayOf(hx)
                                )
                            ) {
                                errors.add("CompensatedDecryption proof failure ${contest.contestId}/${selection.selectionId}")
                            }
                        }

                    } else {
                        errors.add("Must have partialDecryption.proof or missingDecryptions ${contest.contestId}/${selection.selectionId}")
                    }
                }
            }
        }
        val allValid = errors.isEmpty()
        return Stats(tally.tallyId, allValid, ncontests, nselections, errors.joinToString("\n"), nshares)
    }

    fun verifySpoiledBallotTallies(ballots: Iterable<PlaintextTally>, nthreads: Int): StatsAccum {
        val starting = getSystemTimeInMillis()

        runBlocking {
            val verifierJobs = mutableListOf<Job>()
            val ballotProducer = produceTallies(ballots)
            repeat(nthreads) {
                verifierJobs.add(launchVerifier(it, ballotProducer) { ballot -> verifyDecryptedTally(ballot) })
            }

            // wait for all encryptions to be done, then close everything
            joinAll(*verifierJobs.toTypedArray())
        }

        val took = getSystemTimeInMillis() - starting
        val perBallot = (took.toDouble() / count).roundToInt()
        println(" verifySpoiledBallotTallies ok=${globalStat.allOk} took $took millisecs for $count ballots = $perBallot msecs/ballot")
        return globalStat
    }

    private val globalStat = StatsAccum()
    private var count = 0
    private fun CoroutineScope.produceTallies(producer: Iterable<PlaintextTally>): ReceiveChannel<PlaintextTally> = produce {
        for (tally in producer) {
            send(tally)
            yield()
            count++
        }
        channel.close()
    }

    private fun CoroutineScope.launchVerifier(
        id: Int,
        input: ReceiveChannel<PlaintextTally>,
        verify: (PlaintextTally) -> Stats,
    ) = launch(Dispatchers.Default) {
        for (tally in input) {
            if (debugBallots) println("$id channel working on ${tally.tallyId}")
            val stat = verify(tally)
            globalStat.add(stat) // LOOK should be in a mutex
            yield()
        }
    }

}