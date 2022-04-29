package electionguard.workflow

import electionguard.ballot.ElectionInitialized
import electionguard.ballot.PlaintextBallot
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.encrypt.channelEncryption
import electionguard.publish.ElectionRecord
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import electionguard.tally.runAccumulateBallots
import electionguard.verifier.Verifier
import kotlin.test.Test

/** Run workflow starting from ElectionConfig in the start directory (125 selections/ballot). */
class RunWorkflow {
    val configDir = "src/commonTest/data/start"
    val workingDir =  "testOut/runWorkflow"
    val privateDir =  "testOut/runWorkflow/private_data"
    val trusteeDir =  "${privateDir}/trustees"
    val ballotsDir =  "${privateDir}/input"
    val invalidDir =  "${privateDir}/invalid"
    val nballots = 100
    val someAvailable = listOf(1, 2, 5) // 3 of 5 guardians present

    @Test
    fun runWorkflowAllAvailable() {
        val group = productionGroup()
        val present = listOf(1U, 2U, 3U) // all guardians present
        val nguardians = present.maxOf { it }.toInt()
        val quorum = present.count()

        // key ceremony
        val init: ElectionInitialized = runFakeKeyCeremony(group, configDir, workingDir, trusteeDir, nguardians, quorum)
        println("FakeKeyCeremony created ElectionInitialized")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(init.config.manifest, nballots)
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = Publisher(ballotsDir, PublisherMode.createIfMissing)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        // encrypt
        channelEncryption(group, workingDir, workingDir, ballotsDir, invalidDir, true, 11)

        // tally
        runAccumulateBallots(group, workingDir, workingDir, "RunWorkflow")

        // decrypt
        runDecryptingMediator(group, workingDir, workingDir, readDecryptingTrustees(group, trusteeDir, init, present))

        // verify
        println("\nRun Verifier")
        val verifier = Verifier(group, ElectionRecord(workingDir, group))
        val ok = verifier.verify()
        println("Verify is $ok")
    }
}

fun readDecryptingTrustees(
    group: GroupContext,
    trusteeDir: String,
    init: ElectionInitialized,
    present: List<UInt>,
): List<DecryptingTrusteeIF> {
    val consumer = ElectionRecord(trusteeDir, group)
    return init.guardians.filter { present.contains(it.xCoordinate)}.map { consumer.readTrustee(trusteeDir, it.guardianId) }
}