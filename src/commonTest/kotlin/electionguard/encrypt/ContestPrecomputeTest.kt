package electionguard.encrypt

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.submit
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.core.toElementModQ
import electionguard.publish.ElectionRecord
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import electionguard.publish.EncryptedBallotSinkIF
import kotlin.math.roundToInt
import kotlin.test.Test

private val revotes = 0

/*
Relative timings:
BallotPrecompute with 0 revotes per ballot
   Precompute took 8774 millisecs for 11 ballots = 798 msecs/ballot
   Encrypt 482 millisecs for 11 ballots = 44 msecs/ballot

BallotPrecompute with 10 revotes per ballot
   Precompute took 9145 millisecs for 11 ballots = 831 msecs/ballot
   Encrypt 458 millisecs for 11 ballots = 42 msecs/ballot

BallotPrecompute with 20 revotes per ballot
   Precompute took 9601 millisecs for 11 ballots = 873 msecs/ballot
   Encrypt 457 millisecs for 11 ballots = 42 msecs/ballotot

ContestPrecompute with 0 revotes per ballot
   Precompute took 8168 millisecs for 11 ballots = 743 msecs/ballot
   Encrypt 1 millisecs for 11 ballots = 0 msecs/ballot

ContestPrecompute with 10 revotes per ballot
   Precompute took 10620 millisecs for 11 ballots = 965 msecs/ballot
   Encrypt 1 millisecs for 11 ballots = 0 msecs/ballot

ContestPrecompute with 20 revotes per ballot
   Precompute took 12789 millisecs for 11 ballots = 1163 msecs/ballot
   Encrypt 1 millisecs for 11 ballots = 0 msecs/ballot
 */
class ContestPrecomputeTest {
    val electionRecordDir = "src/commonTest/data/runWorkflowAllAvailable"
    val ballotDir = "src/commonTest/data/runWorkflowAllAvailable/private_data/input/"
    val outputDir = "testOut/precompute/contestPrecomputeTest"

    @Test
    fun testContestPrecompute() {
        runTest {
            val group = productionGroup()
            val electionRecordIn = ElectionRecord(electionRecordDir, group)
            val electionInit: ElectionInitialized = electionRecordIn.readElectionInitialized().getOrThrow { IllegalStateException( it ) }
            val ballots = electionRecordIn.iteratePlaintextBallots(ballotDir) { true }

            var starting = getSystemTimeInMillis()
            var count = 0
            val pballots = ballots.map {
                count++
                precomputeContest(group, electionInit, it)
            }
            var took = getSystemTimeInMillis() - starting
            var perBallot = (took.toDouble() / count).roundToInt()
            println("ContestPrecompute with $revotes revotes per ballot")
            println("   Precompute took $took millisecs for ${count} ballots = $perBallot msecs/ballot")

            starting = getSystemTimeInMillis()
            val eballots = pballots.map {
                it.encrypt()
            }
            took = getSystemTimeInMillis() - starting
            perBallot = (took.toDouble() / count).roundToInt()
            println("   Encrypt $took millisecs for ${count} ballots = $perBallot msecs/ballot")

            val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
            val sink: EncryptedBallotSinkIF = publisher.encryptedBallotSink()
            eballots.forEach { sink.writeEncryptedBallot(it.submit(EncryptedBallot.BallotState.CAST)) }
            sink.close()
            println("done")
        }
    }
}

fun precomputeContest(group: GroupContext, electionInit: ElectionInitialized, ballot: PlaintextBallot): ContestPrecompute {
    val manifest: Manifest = electionInit.manifest()
    val codeSeed: ElementModQ = electionInit.cryptoExtendedBaseHash.toElementModQ(group)
    val masterNonce = group.TWO_MOD_Q

    val pballot = ContestPrecompute(
        group,
        manifest,
        ElGamalPublicKey(electionInit.jointPublicKey),
        electionInit.cryptoExtendedBaseHash,
        ballot.ballotId,
        ballot.ballotStyleId,
        codeSeed,
        masterNonce,
        0,
    )

    // now vote in each contest by picking the first selection
    ballot.contests.forEach { contest ->
        val selection = contest.selections[0]
        pballot.vote(contest.contestId, selection.selectionId, 1)
    }

    // simulate revotes in some contests
    for (i in 0 until revotes) {
        val pcontest = pballot.contests[i]
        for (selection in pcontest.selections) {
            if (selection.vote == 0) {
                pcontest.vote(selection.mselection.selectionId, 1)
                break
            }
        }
    }
    return pballot
}