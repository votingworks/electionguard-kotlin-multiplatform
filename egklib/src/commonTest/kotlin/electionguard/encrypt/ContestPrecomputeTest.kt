package electionguard.encrypt

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.core.ElGamalPublicKey
import electionguard.core.GroupContext
import electionguard.core.Stats
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.publish.makeConsumer
import electionguard.verifier.VerifyEncryptedBallots
import kotlin.test.Test
import kotlin.test.assertTrue

class ContestPrecomputeTest {
    val electionRecordDir = "src/commonTest/data/runWorkflowAllAvailable"
    val ballotDir = "src/commonTest/data/runWorkflowAllAvailable/private_data/input/"

    @Test
    fun testContestPrecompute() {
        val group = productionGroup()
        val consumerIn = makeConsumer(electionRecordDir, group)
        val electionInit: ElectionInitialized = consumerIn.readElectionInitialized().getOrThrow { IllegalStateException( it ) }
        val ballots = consumerIn.iteratePlaintextBallots(ballotDir) { true }
        val verifier = VerifyEncryptedBallots(group, electionInit.manifest(), electionInit.jointPublicKey(), electionInit.cryptoExtendedBaseHash(), 25)

        // warm up the cache, ignore results
        val ballot = ballots.iterator().next()
        val pballot = precomputeContest(group, Stats(), 0, electionInit, ballot)
        pballot.encrypt()
        val nselections = pballot.contests.map { it.selections.size }.sum()
        println("\nContestPrecomputeTest $nselections selections/ballot")

        for (revotes in 0..60 step 60) {
            println("\nTest $revotes revotes/ballot")
            val stats = Stats()
            val pballots = ballots.map {
                precomputeContest(group, stats, revotes, electionInit, it)
            }

            val starting = getSystemTimeInMillis()
            val eballots = pballots.map {
                it.encrypt()
            }
            stats.of("latency", "ballot", "").accum(getSystemTimeInMillis() - starting, eballots.size)
            stats.show()

            val verifyResult = verifier.verify(eballots.map { it.cast() }, stats)
            assertTrue(verifyResult is Ok)
        }
        println("done")

    }
}

fun precomputeContest(group: GroupContext, stats: Stats, revotes: Int, electionInit: ElectionInitialized, ballot: PlaintextBallot): ContestPrecompute {
    val manifest: Manifest = electionInit.manifest()

    var starting = getSystemTimeInMillis()
    val pballot = ContestPrecompute(
        group,
        manifest,
        ElGamalPublicKey(electionInit.jointPublicKey),
        electionInit.extendedBaseHash,
        ballot.ballotId,
        ballot.ballotStyleId,
        group.randomElementModQ(2),
        null,
        null,
    )
    val nselections = pballot.contests.map { it.selections.size }.sum()

    // now vote in each contest by picking the first selection
    ballot.contests.forEach { contest ->
        val selection = contest.selections[0]
        pballot.vote(contest.contestId, selection.selectionId, 1)
    }
    stats.of("precompute", "selection").accum(getSystemTimeInMillis() - starting, nselections)

    // simulate revotes in some contests
    starting = getSystemTimeInMillis()
    val ncontests = ballot.contests.size
    for (count in 0 until revotes) {
        val pcontest = pballot.contests[count % ncontests]
        for (selection in pcontest.selections) {
            if (selection.vote == 0) {
                pcontest.vote(selection.mselection.selectionId, 1)
                break
            }
        }
    }
    stats.of("revotes", "revote").accum(getSystemTimeInMillis() - starting, revotes)

    return pballot
}