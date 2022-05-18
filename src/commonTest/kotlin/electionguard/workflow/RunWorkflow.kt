package electionguard.workflow

import electionguard.ballot.ElectionInitialized
import electionguard.ballot.PlaintextBallot
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.decrypt.runDecryptingMediator
import electionguard.encrypt.batchEncryption
import electionguard.publish.ElectionRecord
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import electionguard.tally.runAccumulateBallots
import electionguard.verifier.Verifier
import kotlin.test.Test

/** Run workflow starting from ElectionConfig in the start directory (125 selections/ballot). */
class RunWorkflow {
    val configDir = "src/commonTest/data/start"
    val nballots = 100

    @Test
    fun runWorkflowAllAvailable() {
        val workingDir =  "testOut/runWorkflowAllAvailable"
        val privateDir =  "testOut/runWorkflowAllAvailable/private_data"
        val trusteeDir =  "${privateDir}/trustees"
        val ballotsDir =  "${privateDir}/input"
        val invalidDir =  "${privateDir}/invalid"

        val group = productionGroup()
        val present = listOf(1U, 2U, 3U) // all guardians present
        val nguardians = present.maxOf { it }.toInt()
        val quorum = present.count()

        // key ceremony
        val init: ElectionInitialized = runFakeKeyCeremony(group, configDir, workingDir, trusteeDir, nguardians, quorum)
        println("FakeKeyCeremony created ElectionInitialized, guardians = $present")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(init.config.manifest, nballots)
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = Publisher(ballotsDir, PublisherMode.createIfMissing)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        // encrypt
        batchEncryption(group, workingDir, workingDir, ballotsDir, invalidDir, true, 11)

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

    @Test
    fun runWorkflowSomeAvailable() {
        val workingDir =  "testOut/runWorkflowSomeAvailable"
        val privateDir =  "testOut/runWorkflowSomeAvailable/private_data"
        val trusteeDir =  "${privateDir}/trustees"
        val ballotsDir =  "${privateDir}/input"
        val invalidDir =  "${privateDir}/invalid"

        val group = productionGroup()
        val present = listOf(1U, 2U, 5U) // 3 of 5 guardians present
        val nguardians = present.maxOf { it }.toInt()
        val quorum = present.count()

        // key ceremony
        val init: ElectionInitialized = runFakeKeyCeremony(group, configDir, workingDir, trusteeDir, nguardians, quorum)
        println("FakeKeyCeremony created ElectionInitialized, guardians = $present")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(init.config.manifest, nballots)
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = Publisher(ballotsDir, PublisherMode.createIfMissing)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        // encrypt
        batchEncryption(group, workingDir, workingDir, ballotsDir, invalidDir, true, 11)

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