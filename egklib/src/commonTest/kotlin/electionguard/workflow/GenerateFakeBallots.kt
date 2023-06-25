package electionguard.workflow

import electionguard.ballot.PlaintextBallot
import electionguard.core.productionGroup
import electionguard.input.RandomBallotProvider
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import kotlin.test.Test

/** Generate fake ballots for testing. No actual testing here. */
class GenerateFakeBallots {
    val inputDir = "src/commonTest/data/startConfigProto"
    val outputDirJson =  "testOut/fakeBallots/json"
    val outputDirProto =  "testOut/fakeBallots/proto"

    @Test
    fun generateFakeBallotsJson() {
        val group = productionGroup()
        val electionRecord = readElectionRecord(group, inputDir)

        val nballots = 33

        val ballotProvider = RandomBallotProvider(electionRecord.manifest(), nballots)
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()

        val publisher = makePublisher(outputDirJson, true, true)
        publisher.writePlaintextBallot(outputDirJson, ballots)

        val publisherProto = makePublisher(outputDirProto, true, false)
        publisherProto.writePlaintextBallot(outputDirProto, ballots)
    }
}