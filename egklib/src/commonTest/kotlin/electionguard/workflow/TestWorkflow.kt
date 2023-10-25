package electionguard.workflow

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.PlaintextBallot
import electionguard.cli.RunAccumulateTally.Companion.runAccumulateBallots
import electionguard.cli.RunBatchEncryption.Companion.batchEncryption
import electionguard.cli.RunTrustedBallotDecryption.Companion.runDecryptBallots
import electionguard.cli.RunTrustedTallyDecryption.Companion.runDecryptTally
import electionguard.core.GroupContext
import electionguard.core.Stats
import electionguard.core.productionGroup
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.encrypt.AddEncryptedBallot
import electionguard.input.RandomBallotProvider
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import electionguard.publish.makeTrusteeSource
import electionguard.verifier.Verifier
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Run complete workflow starting from ElectionConfig in the start directory, all the way through verify.
 * (See RunCreateTestManifestTest to regenerate Manifest)
 * (See RunCreateConfigTest/RunCreateElectionConfig to regenerate ElectionConfig)
 * Note that TestWorkflow uses RunFakeKeyCeremonyTest, not real KeyCeremony.
 *  1. The results can be copied to the test data sets "src/commonTest/data/workflow" whenever the
 *     election record changes.
 *  2. Now that we have fixed balloty ids, dont need to RunDecryptBallotsTest for more damn things to do.
 */
class TestWorkflow {
    private val configDirProto = "src/commonTest/data/startConfigProto"
    private val configDirJson = "src/commonTest/data/startConfigJson"
    private val nballots = 11
    private val nthreads = 11

    @Test
    fun runWorkflowAllAvailable() {
        val workingDir =  "testOut/workflow/allAvailableProto"
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
        val (manifest, init) = runFakeKeyCeremony(group, configDirProto, workingDir, trusteeDir, nguardians, quorum, false)
        println("FakeKeyCeremony created ElectionInitialized, guardians = $present")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(manifest, nballots).withSequentialIds()
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = makePublisher(ballotsDir)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        // encrypt
        batchEncryption(group, workingDir, workingDir, ballotsDir, invalidDir, "runWorkflowAllAvailable", nthreads, "createdBy")

        // tally
        runAccumulateBallots(group, workingDir, workingDir, "RunWorkflow", "createdBy")

        // decrypt tally
        runDecryptTally(group, workingDir, workingDir, readDecryptingTrustees(group, trusteeDir, init, present, false), "runWorkflowAllAvailable")

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
    fun runWorkflowChained() {
        val workingDir =  "testOut/workflow/chainedProto"
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
        val (manifest, electionInit) = runFakeKeyCeremony(group, configDirProto, workingDir, trusteeDir, nguardians, quorum, true)
        println("FakeKeyCeremony created ElectionInitialized, guardians = $present")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(manifest, nballots).withSequentialIds()
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = makePublisher(ballotsDir)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        // encrypt
        val encryptor = AddEncryptedBallot(
            group,
            manifest,
            electionInit,
            "device009",
            workingDir,
            invalidDir,
            isJson = publisher.isJson(),
        )
        var count = 1
        ballots.forEach { ballot ->
            val state = if (count % (nballots/3) == 0) EncryptedBallot.BallotState.SPOILED else EncryptedBallot.BallotState.CAST
            val result = encryptor.encrypt(ballot)
            assertTrue(result is Ok)
            encryptor.submit(result.unwrap().confirmationCode, state)
            println(" write ${ballot.ballotId} $state")
            count++
        }
        encryptor.close()

        // tally
        runAccumulateBallots(group, workingDir, workingDir, "RunWorkflow", "createdBy")

        // decrypt tally
        val trustees = readDecryptingTrustees(group, trusteeDir, electionInit, present, false)
        runDecryptTally(group, workingDir, workingDir, trustees, "runWorkflowChained")

        // decrypt spoiled ballots
        val decryptedBallots = runDecryptBallots(group, workingDir, workingDir, trustees,null, nthreads)
        println(" Number of decryptedBallots $decryptedBallots")

        // verify
        println("\nRun Verifier on $workingDir")
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
        val workingDir =  "testOut/workflow/someAvailableProto"
        val privateDir =  "$workingDir/private_data"
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
        val (manifest, init) = runFakeKeyCeremony(group, configDirProto, workingDir, trusteeDir, nguardians, quorum, false)
        println("FakeKeyCeremony created ElectionInitialized, nguardians = $nguardians quorum = $quorum")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(manifest, nballots).withSequentialIds()
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = makePublisher(ballotsDir)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        // encrypt
        batchEncryption(group, workingDir, workingDir, ballotsDir, invalidDir, "runWorkflowSomeAvailable", nthreads, "createdBy")

        // tally
        runAccumulateBallots(group, workingDir, workingDir, "RunWorkflow", "createdBy")

        // decrypt
        runDecryptTally(group, workingDir, workingDir, readDecryptingTrustees(group, trusteeDir, init, present, false), "runWorkflowSomeAvailable")

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
        val (manifest, init) = runFakeKeyCeremony(group, configDirJson, workingDir, trusteeDir, nguardians, quorum, false)
        println("FakeKeyCeremony created ElectionInitialized, guardians = $present")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(manifest, nballots).withSequentialIds()
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = makePublisher(ballotsDir, false, true)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        // encrypt
        batchEncryption(group, workingDir, workingDir, ballotsDir, invalidDir, "runWorkflowAllAvailableJson", nthreads, "createdBy")

        // tally
        runAccumulateBallots(group, workingDir, workingDir, "RunWorkflow", "createdBy")

        // decrypt tally
        runDecryptTally(group, workingDir, workingDir, readDecryptingTrustees(group, trusteeDir, init, present, true), "runWorkflowAllAvailableJson")

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
    fun runWorkflowChainedJson() {
        val workingDir =  "testOut/workflow/chainedJson"
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
        val (manifest, electionInit) = runFakeKeyCeremony(group, configDirJson, workingDir, trusteeDir, nguardians, quorum, true)
        println("FakeKeyCeremony created ElectionInitialized, guardians = $present")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(manifest, nballots).withSequentialIds()
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = makePublisher(ballotsDir, false, true)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        // encrypt
        val encryptor = AddEncryptedBallot(
            group,
            manifest,
            electionInit,
            "runWorkflowChainedJson",
            workingDir,
            invalidDir,
            isJson = publisher.isJson(),
        )
        var count = 1
        ballots.forEach { ballot ->
            val state = if (count % (nballots/3) == 0) EncryptedBallot.BallotState.SPOILED else EncryptedBallot.BallotState.CAST
            val result = encryptor.encrypt(ballot)
            assertTrue(result is Ok)
            encryptor.submit(result.unwrap().confirmationCode, state)
            println(" write ${ballot.ballotId} $state")
            count++
        }
        encryptor.close()

        // tally
        runAccumulateBallots(group, workingDir, workingDir, "runWorkflowChainedJson", "runWorkflowChainedJson")

        // decrypt tally
        val trustees = readDecryptingTrustees(group, trusteeDir, electionInit, present, true)
        runDecryptTally(group, workingDir, workingDir, trustees, "runWorkflowChainedJson")

        // decrypt spoiled ballots
        val decryptedBallots = runDecryptBallots(group, workingDir, workingDir, trustees,null, nthreads)
        println(" Number of decryptedBallots $decryptedBallots")

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
        val (manifest, init) = runFakeKeyCeremony(group, configDirJson, workingDir, trusteeDir, nguardians, quorum, false)
        println("FakeKeyCeremony created ElectionInitialized, nguardians = $nguardians quorum = $quorum")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(manifest, nballots).withSequentialIds()
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = makePublisher(ballotsDir, false, true)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        // encrypt
        batchEncryption(group, workingDir, workingDir, ballotsDir, invalidDir, "runWorkflowSomeAvailableJson", nthreads, "createdBy")

        // tally
        runAccumulateBallots(group, workingDir, workingDir, "RunWorkflow", "createdBy")

        // decrypt
        runDecryptTally(group, workingDir, workingDir, readDecryptingTrustees(group, trusteeDir, init, present, true), "runWorkflowSomeAvailableJson")

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