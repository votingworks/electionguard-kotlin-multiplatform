package electionguard.workflow

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.PlaintextBallot
import electionguard.core.productionGroup
import electionguard.input.RandomBallotProvider
import electionguard.publish.Consumer
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlin.test.Test

/** Run a fake KeyCeremony to generate an ElectionInitialized for workflow testing. */
class RunFakeBallotTest {

    @Test
    fun runFakeBallotTest() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/runWorkflowAllAvailable"
        val outputDir =  "testOut/runFakeBallotTest/private_data"
        val nballots = 33

        val consumerIn = Consumer(inputDir, group)
        val init: ElectionInitialized = consumerIn.readElectionInitialized().getOrThrow { IllegalStateException( it ) }

        val ballotProvider = RandomBallotProvider(init.config.manifest, nballots)
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()

        val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
        publisher.writePlaintextBallot(outputDir, ballots)
    }
}