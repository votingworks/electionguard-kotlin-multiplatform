package electionguard.workflow

import electionguard.ballot.ElectionInitialized
import electionguard.ballot.PlaintextBallot
import electionguard.core.GroupContext
import electionguard.core.Stats
import electionguard.core.productionGroup
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.decrypt.runDecryptTally
import electionguard.encrypt.batchEncryption
import electionguard.input.RandomBallotProvider
import electionguard.publish.Consumer
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import electionguard.publish.electionRecordFromConsumer
import electionguard.tally.runAccumulateBallots
import electionguard.verifier.Verifier
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Run complete workflow starting from ElectionConfig in the start directory, all the way through verify.
 * (Uses RunFakeKeyCeremonyTest, not real KeyCeremony).
 * The results can be copied to the test data sets "src/commonTest/data/runWorkflowAll(Some)Available" whenever the
 * election record changes.
 *   1. generate with jvm for src/commonTest/data/runWorkflowAll(Some)Available
 *   2. generate with native for src/commonTest/data/testElectionRecord/native/
 *   3. see RunDecryptBallotsTest for another damn thing to do
 *   4. replace src/commonTest/data/runWorkflowSomeAvailable/spoiledBallotTallies.protobuf if needed, which is a
 *      DecryptedBallotOrTally, by running RunDecryptBallotsTest.testDecryptBallotsSomeFromList and copying that
 *      file.
 */
class TestWorkflow {
    private val configDir = "src/commonTest/data/start"
    private val nballots = 100
    private val nthreads = 25

    @Test
    fun runWorkflowAllAvailable() {
        val workingDir =  "testOut/runWorkflowAllAvailable"
        val privateDir =  "testOut/runWorkflowAllAvailable/private_data"
        val trusteeDir =  "${privateDir}/trustees"
        val ballotsDir =  "${privateDir}/input"
        val invalidDir =  "${privateDir}/invalid"

        val group = productionGroup()
        val present = listOf(1, 2, 3) // all guardians present
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
        batchEncryption(group, workingDir, workingDir, ballotsDir, invalidDir, true, nthreads, "createdBy")

        // tally
        runAccumulateBallots(group, workingDir, workingDir, "RunWorkflow", "createdBy")

        // decrypt tally
        runDecryptTally(group, workingDir, workingDir, readDecryptingTrustees(group, trusteeDir, init, present), "createdBy")

        // verify
        println("\nRun Verifier")
        val record = electionRecordFromConsumer(Consumer(workingDir, group))
        val verifier = Verifier(record)
        val stats = Stats()
        val ok = verifier.verify(stats)
        stats.show()
        println("Verify is $ok")
        assertTrue(ok)
    }

// LOOK do a test where there are 4 available in a 3 of 5

    @Test
    fun runWorkflowSomeAvailable() {
        val workingDir =  "testOut/runWorkflowSomeAvailable"
        val privateDir =  "testOut/runWorkflowSomeAvailable/private_data"
        val trusteeDir =  "${privateDir}/trustees"
        val ballotsDir =  "${privateDir}/input"
        val invalidDir =  "${privateDir}/invalid"

        val group = productionGroup()
        val present = listOf(1, 2, 5) // 3 of 5 guardians present
        val nguardians = present.maxOf { it }.toInt()
        val quorum = present.count()

        // key ceremony
        val init: ElectionInitialized = runFakeKeyCeremony(group, configDir, workingDir, trusteeDir, nguardians, quorum)
        println("FakeKeyCeremony created ElectionInitialized, nguardians = $nguardians quorum = $quorum")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(init.config.manifest, nballots)
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = Publisher(ballotsDir, PublisherMode.createIfMissing)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        // encrypt
        batchEncryption(group, workingDir, workingDir, ballotsDir, invalidDir, true, nthreads, "createdBy")

        // tally
        runAccumulateBallots(group, workingDir, workingDir, "RunWorkflow", "createdBy")

        // decrypt
        runDecryptTally(group, workingDir, workingDir, readDecryptingTrustees(group, trusteeDir, init, present), null)

        // verify
        println("\nRun Verifier")
        val record = electionRecordFromConsumer(Consumer(workingDir, group))
        val verifier = Verifier(record)
        val stats = Stats()
        val ok = verifier.verify(stats)
        stats.show()
        println("Verify is $ok")
        assertTrue(ok)
    }
}

fun readDecryptingTrustees(
    group: GroupContext,
    trusteeDir: String,
    init: ElectionInitialized,
    present: List<Int>,
): List<DecryptingTrusteeIF> {
    val consumer = Consumer(trusteeDir, group)
    return init.guardians.filter { present.contains(it.xCoordinate)}.map { consumer.readTrustee(trusteeDir, it.guardianId) }
}