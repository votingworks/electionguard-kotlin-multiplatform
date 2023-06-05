package electionguard.workflow

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.PlaintextBallot
import electionguard.core.productionGroup
import electionguard.input.RandomBallotProvider
import electionguard.publish.makeConsumer
import electionguard.publish.makePublisher
import kotlin.test.Test

/** Generate fake ballots for testing. No actual testing here. */
class GenerateFakeBallots {

    @Test
    fun generateFakeBallots() {
        val group = productionGroup()
        val inputDir = "testOut/RunKeyCeremonyTest" // "src/commonTest/data/runWorkflowAllAvailable"
        val outputDir =  "testOut/GenerateFakeBallots/private_data"
        val nballots = 33

        val consumerIn = makeConsumer(inputDir, group)
        val init: ElectionInitialized = consumerIn.readElectionInitialized().getOrThrow { IllegalStateException( it ) }

        val ballotProvider = RandomBallotProvider(init.config.manifest, nballots)
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()

        val publisher = makePublisher(outputDir)
        publisher.writePlaintextBallot(outputDir, ballots)
    }
}