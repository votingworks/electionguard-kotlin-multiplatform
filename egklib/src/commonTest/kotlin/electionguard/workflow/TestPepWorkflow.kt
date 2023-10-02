package electionguard.workflow

import com.github.michaelbull.result.Ok
import electionguard.cli.RunBatchEncryption.Companion.batchEncryption
import electionguard.cli.RunCreateElectionConfig
import electionguard.core.*
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.decrypt.Guardians
import electionguard.decrypt.PepSimple
import electionguard.decrypt.PepTrusted
import electionguard.publish.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * Run workflow with varying number of guardians, on the same ballots, and compare the results.
 */
class TestPepWorkflow {
    val group = productionGroup()

    private val manifestJson = "src/commonTest/data/startManifestJson/manifest.json"
    private val inputBallotDir = "src/commonTest/data/fakeBallots/json"
    val name1 = "runWorkflowOneGuardian"
    val name2 = "runWorkflowThreeGuardian"
    val name3 = "runWorkflow5of6Guardian"
    val name4 = "runWorkflow8of10Guardian"

    @Test
    fun runWorkflows() {
        println("productionGroup (Default) = $group class = ${group.javaClass.name}")
        runWorkflow(name1, 1, 1, listOf(1), 1)
        //runWorkflow(name1, 1, 1, listOf(1), 25)

        //runWorkflow(name2, 3, 3, listOf(1,2,3), 1)
        //runWorkflow(name2, 3, 3, listOf(1,2,3), 25)

        //runWorkflow(name3, 6, 5, listOf(1,2,4,5,6), 1)
        //runWorkflow(name3, 6, 5, listOf(1,2,4,5,6), 25)

        //runWorkflow(name4, 10, 8, listOf(1,2,4,5,6,7,8,9), 1)
        //runWorkflow(name4, 10, 8, listOf(1,2,4,5,6,7,8,9), 25)
    }

    fun runWorkflow(name : String, nguardians: Int, quorum: Int, present: List<Int>, nthreads: Int) {
        println("===========================================================")
        val workingDir =  "testOut/workflow/$name"
        val privateDir =  "$workingDir/private_data"
        val trusteeDir =  "${privateDir}/trustees"
        val invalidDir =  "${privateDir}/invalid"

        // delete current workingDir
        makePublisher(workingDir, true)

        RunCreateElectionConfig.main(
            arrayOf(
                "-manifest", manifestJson,
                "-nguardians", nguardians.toString(),
                "-quorum", quorum.toString(),
                "-out", workingDir,
                "-device", "device11",
                "-createdBy", name1,
            )
        )

        // key ceremony
        val (manifest, init) = runFakeKeyCeremony(group, workingDir, workingDir, trusteeDir, nguardians, quorum, false)

        // encrypt
        group.showAndClearCountPowP()
        batchEncryption(group, workingDir, workingDir, inputBallotDir, invalidDir, "device11", nthreads, name1)
        //println("----------- after encrypt ${group.showAndClearCountPowP()}")

        // encrypt again, simulating the CAKE workflow of scanning the paper ballots
        batchEncryption(group, workingDir, workingDir, inputBallotDir, invalidDir, "scanPaper", nthreads, name1)
        //println("----------- after encrypt ${group.showAndClearCountPowP()}")

        val dtrustees : List<DecryptingTrusteeIF> = readDecryptingTrustees(group, trusteeDir, init, present, true)

        // todo PARELLIZE PEP
        // call PEP to compare the two encryptions
        val starting = getSystemTimeInMillis() // wall clock
        val pep = PepTrusted(
            group,
            init.extendedBaseHash,
            ElGamalPublicKey(init.jointPublicKey),
            Guardians(group, init.guardians), // all guardians
            dtrustees,
            3
        )
        group.showAndClearCountPowP()
        compareBallotPepEquivilence(pep, workingDir, "device11", "scanPaper")

        val took = getSystemTimeInMillis() - starting
        val nballots = 33
        val per = took / (1000.0 * nballots)
        println("\nPEP took $took msecs = ${per} secs/ballot")
        val nencyptions = 100
        val expect = (8 * nguardians + 12) * nencyptions * nballots
        println("----------- after compareBallotPepEquivilence ${group.showAndClearCountPowP()}, expect=$expect")
    }

    fun compareBallotPepEquivilence(pep: PepTrusted, workingDir : String, device1 : String, device2 : String) {
        val record =  readElectionRecord(group, workingDir)

        val ballotsa = record.encryptedBallots(device1) { true }.iterator()
        val ballotsb = record.encryptedBallots(device2) { true }.iterator()
        while (ballotsa.hasNext()) { // assumes same order, may be wrong
            val ballota = ballotsa.next()
            val result = pep.testEquivalent(ballota, ballotsb.next())
            assertTrue(result is Ok)
            // println(" ${ballota.ballotId} compare ${result}")
            assertEquals(true, result.value)
        }
    }
}