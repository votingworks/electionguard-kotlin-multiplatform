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
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import electionguard.publish.makeConsumer
import electionguard.publish.makeTrusteeSource
import electionguard.tally.runAccumulateBallots
import electionguard.verifier.Verifier
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Run complete workflow starting from ElectionConfig in the start directory, all the way through verify.
 * (See RunCreateConfigTest/RunCreateElectionConfig to regenerate ElectionConfig)
 * TestWorkflow uses RunFakeKeyCeremonyTest, not real KeyCeremony.
 *  1. The results can be copied to the test data sets "src/commonTest/data/all(some)Available(Json)" whenever the
 *     election record changes.
 *  2. Then see RunDecryptBallotsTest for more damn things to do
 *  3. Replace src/commonTest/data/someAvailable/challengedBallotTallies.protobuf, by running
 *     RunDecryptBallotsTest.testDecryptBallotsSomeFromList and copying that file.
 *  4. Replace src/commonTest/data/someAvailableJson/challenged_ballots/dballot-*.json by running
 *      RunDecryptBallotsJsonTest.testDecryptBallotsSomeFromList and copying that directory.
 *  5. Replace src/commonTest/data/testElectionRecord/jsonZip/json25.zip by going inside
 *      testOut/workflow/allAvailableJson and zipping that directory, and copying that file.
 */
class TestWorkflow {
    private val configDir = "src/commonTest/data/startConfigProto"
    private val configDirJson = "src/commonTest/data/startConfigJson"
    private val nballots = 11
    private val nthreads = 11

    @Test
    fun runWorkflowAllAvailable() {
        val workingDir =  "testOut/workflow/allAvailable"
        val privateDir =  "$workingDir/private_data"
        val trusteeDir =  "${privateDir}/trustees"
        val ballotsDir =  "${privateDir}/input"
        val invalidDir =  "${privateDir}/invalid"

        val group = productionGroup()
        val present = listOf(1, 2, 3) // all guardians present
        val nguardians = present.maxOf { it }.toInt()
        val quorum = present.count()

        // delete current workingDir
        makePublisher(workingDir, true)

        // key ceremony
        val (manifest, init) = runFakeKeyCeremony(group, configDir, workingDir, trusteeDir, nguardians, quorum)
        println("FakeKeyCeremony created ElectionInitialized, guardians = $present")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(manifest, nballots)
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = makePublisher(ballotsDir)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        // encrypt
        batchEncryption(group, workingDir, workingDir, ballotsDir, invalidDir, true, nthreads, "createdBy")

        // tally
        runAccumulateBallots(group, workingDir, workingDir, "RunWorkflow", "createdBy")

        // decrypt tally
        runDecryptTally(group, workingDir, workingDir, readDecryptingTrustees(group, trusteeDir, init, present, false), "createdBy")

        // verify
        println("\nRun Verifier")
        val record = readElectionRecord(group, workingDir)
        val verifier = Verifier(record)
        val stats = Stats()
        val ok = verifier.verify(stats)
        stats.show()
        println("Verify is $ok")
        assertTrue(ok)
    }

    @Test
    fun runWorkflowSomeAvailable() {
        val workingDir =  "testOut/workflow/someAvailable"
        val privateDir =  "testOut/workflow/someAvailable/private_data"
        val trusteeDir =  "${privateDir}/trustees"
        val ballotsDir =  "${privateDir}/input"
        val invalidDir =  "${privateDir}/invalid"

        val group = productionGroup()
        val present = listOf(1, 2, 5) // 3 of 5 guardians present
        val nguardians = present.maxOf { it }.toInt()
        val quorum = present.count()

        // delete current workingDir
        makePublisher(workingDir, true)

        // key ceremony
        val (manifest, init) = runFakeKeyCeremony(group, configDir, workingDir, trusteeDir, nguardians, quorum)
        println("FakeKeyCeremony created ElectionInitialized, nguardians = $nguardians quorum = $quorum")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(manifest, nballots)
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = makePublisher(ballotsDir)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        // encrypt
        batchEncryption(group, workingDir, workingDir, ballotsDir, invalidDir, true, nthreads, "createdBy")

        // tally
        runAccumulateBallots(group, workingDir, workingDir, "RunWorkflow", "createdBy")

        // decrypt
        runDecryptTally(group, workingDir, workingDir, readDecryptingTrustees(group, trusteeDir, init, present, false), null)

        // verify
        println("\nRun Verifier")
        val record = readElectionRecord(group, workingDir)
        val verifier = Verifier(record)
        val stats = Stats()
        val ok = verifier.verify(stats)
        stats.show()
        println("Verify is $ok")
        assertTrue(ok)
    }

    @Test
    fun runWorkflowAllAvailableJson() {
        val workingDir =  "testOut/workflow/allAvailableJson"
        val privateDir =  "$workingDir/private_data"
        val trusteeDir =  "${privateDir}/trustees"
        val ballotsDir =  "${privateDir}/input"
        val invalidDir =  "${privateDir}/invalid"

        val group = productionGroup()
        val present = listOf(1, 2, 3) // all guardians present
        val nguardians = present.maxOf { it }.toInt()
        val quorum = present.count()

        // delete current workingDir
        makePublisher(workingDir, true)

        // key ceremony
        val (manifest, init) = runFakeKeyCeremony(group, configDirJson, workingDir, trusteeDir, nguardians, quorum)
        println("FakeKeyCeremony created ElectionInitialized, guardians = $present")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(manifest, nballots)
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = makePublisher(ballotsDir, false, true)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        // encrypt
        batchEncryption(group, workingDir, workingDir, ballotsDir, invalidDir, true, nthreads, "createdBy")

        // tally
        runAccumulateBallots(group, workingDir, workingDir, "RunWorkflow", "createdBy")

        // decrypt tally
        runDecryptTally(group, workingDir, workingDir, readDecryptingTrustees(group, trusteeDir, init, present, true), "createdBy")

        // verify
        println("\nRun Verifier")
        val record = readElectionRecord(group, workingDir)
        val verifier = Verifier(record)
        val stats = Stats()
        val ok = verifier.verify(stats)
        stats.show()
        println("Verify is $ok")
        assertTrue(ok)
    }

    @Test
    fun runWorkflowSomeAvailableJson() {
        val workingDir =  "testOut/workflow/someAvailableJson"
        val privateDir =  "$workingDir/private_data"
        val trusteeDir =  "${privateDir}/trustees"
        val ballotsDir =  "${privateDir}/input"
        val invalidDir =  "${privateDir}/invalid"

        // delete current workingDir
        makePublisher(workingDir, true)

        val group = productionGroup()
        val present = listOf(1, 2, 5) // 3 of 5 guardians present
        val nguardians = present.maxOf { it }.toInt()
        val quorum = present.count()

        // key ceremony
        val (manifest, init) = runFakeKeyCeremony(group, configDirJson, workingDir, trusteeDir, nguardians, quorum)
        println("FakeKeyCeremony created ElectionInitialized, nguardians = $nguardians quorum = $quorum")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(manifest, nballots)
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = makePublisher(ballotsDir, false, true)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        // encrypt
        batchEncryption(group, workingDir, workingDir, ballotsDir, invalidDir, true, nthreads, "createdBy")

        // tally
        runAccumulateBallots(group, workingDir, workingDir, "RunWorkflow", "createdBy")

        // decrypt
        runDecryptTally(group, workingDir, workingDir, readDecryptingTrustees(group, trusteeDir, init, present, true), null)

        // verify
        println("\nRun Verifier")
        val record = readElectionRecord(group, workingDir)
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
    isJson: Boolean,
): List<DecryptingTrusteeIF> {
    val consumer = makeTrusteeSource(trusteeDir, group, isJson)
    return init.guardians.filter { present.contains(it.xCoordinate)}.map { consumer.readTrustee(trusteeDir, it.guardianId) }
}