package webapps.electionguard.decrypt

import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.PlaintextBallot
import electionguard.core.GroupContext
import electionguard.core.Stats
import electionguard.core.productionGroup
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.encrypt.batchEncryption
import electionguard.input.RandomBallotProvider
import electionguard.publish.makePublisher
import electionguard.publish.electionRecordFromConsumer
import electionguard.publish.makeConsumer
import electionguard.tally.runAccumulateBallots
import electionguard.verifier.Verifier
import kotlin.test.Test
import kotlin.test.assertTrue

class TestRemoteWorkflow {
    val remoteUrl = "http://0.0.0.0:11190"
    val keyceremonyDir =  "/home/snake/tmp/electionguard/RunRemoteKeyCeremonyTest"
    val trusteeDir =  "${keyceremonyDir}/private_data/trustees"
    private val nballots = 25
    private val nthreads = 25

    @Test
    fun runWorkflowAllAvailable() {
        val workingDir =  "/home/snake/tmp/electionguard/RunRemoteWorkflowAll"
        val privateDir =  "$workingDir/private_data"
        val ballotsDir =  "${privateDir}/input"
        val invalidDir =  "${privateDir}/invalid"

        val group = productionGroup()

        // key ceremony was already run in RunRemoteKeyCeremonyTest
        val consumerIn = makeConsumer(keyceremonyDir, group)
        val init: ElectionInitialized = consumerIn.readElectionInitialized().unwrap()
        println("ElectionInitialized read from $keyceremonyDir")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(init.config.manifest, nballots)
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = makePublisher(ballotsDir)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        // encrypt
        batchEncryption(group, keyceremonyDir, workingDir, ballotsDir, invalidDir, true, nthreads, "createdBy")

        // tally
        runAccumulateBallots(group, workingDir, workingDir, "RunWorkflow", "createdBy")

        // decrypt tally
        runRemoteDecrypt(group, workingDir, trusteeDir, workingDir, remoteUrl, null, "RunRemoteWorkflowAll")

        // verify
        println("\nRun Verifier")
        val record = electionRecordFromConsumer(makeConsumer(workingDir, group))
        val verifier = Verifier(record)
        val stats = Stats()
        val ok = verifier.verify(stats)
        stats.show()
        println("Verify is $ok")
        assertTrue(ok)
    }

    @Test
    fun runWorkflowSomeAvailable() {
        val workingDir =  "/home/snake/tmp/electionguard/RunRemoteWorkflowSome"
        val privateDir =  "$workingDir/private_data"
        val ballotsDir =  "${privateDir}/input"
        val invalidDir =  "${privateDir}/invalid"

        val group = productionGroup()

        // key ceremony was already run in RunRemoteKeyCeremonyTest
        val consumerIn = makeConsumer(keyceremonyDir, group)
        val init: ElectionInitialized = consumerIn.readElectionInitialized().unwrap()
        println("ElectionInitialized read from  $keyceremonyDir")

        // create fake ballots
        val ballotProvider = RandomBallotProvider(init.config.manifest, nballots)
        val ballots: List<PlaintextBallot> = ballotProvider.ballots()
        val publisher = makePublisher(ballotsDir)
        publisher.writePlaintextBallot(ballotsDir, ballots)
        println("RandomBallotProvider created ${ballots.size} ballots")

        // encrypt
        batchEncryption(group, keyceremonyDir, workingDir, ballotsDir, invalidDir, true, nthreads, "createdBy")

        // tally
        runAccumulateBallots(group, workingDir, workingDir, "RunWorkflow", "createdBy")

        // decrypt
        runRemoteDecrypt(group, workingDir, trusteeDir, workingDir, remoteUrl, "2,4", "RunRemoteWorkflowSome")

        // verify
        println("\nRun Verifier")
        val record = electionRecordFromConsumer(makeConsumer(workingDir, group))
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
    val consumer = makeConsumer(trusteeDir, group)
    return init.guardians.filter { present.contains(it.xCoordinate)}.map { consumer.readTrustee(trusteeDir, it.guardianId) }
}