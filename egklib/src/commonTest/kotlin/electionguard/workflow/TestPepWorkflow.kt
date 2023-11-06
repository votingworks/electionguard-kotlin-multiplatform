package electionguard.workflow

import electionguard.cli.RunBatchEncryption.Companion.batchEncryption
import electionguard.cli.RunCreateElectionConfig
import electionguard.cli.RunTrustedPep
import electionguard.core.*
import electionguard.publish.*
import kotlin.test.Test

/**
 * Run workflow with varying number of guardians, on the same ballots.
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
    fun runPepWorkflows() {
        println("productionGroup (Default) = $group class = ${group.javaClass.name}")
        //runPepWorkflow(name1, 1, 1, listOf(1), 1)
        runPepWorkflow(name1, 1, 1, listOf(1), 25)

        //runPepWorkflow(name2, 3, 3, listOf(1,2,3), 1)
        runPepWorkflow(name2, 3, 3, listOf(1,2,3), 25)

        //runPepWorkflow(name3, 6, 5, listOf(1,2,4,5,6), 1)
        runPepWorkflow(name3, 6, 5, listOf(1,2,4,5,6), 25)

        //runPepWorkflow(name4, 10, 8, listOf(1,2,4,5,6,7,8,9), 1)
        runPepWorkflow(name4, 10, 8, listOf(1,2,4,5,6,7,8,9), 25)
    }

    fun runPepWorkflow(name : String, nguardians: Int, quorum: Int, present: List<Int>, nthreads: Int) {
        println("======================================================================================")
        val workingDir =  "testOut/workflowPep/$name"
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
        val (_, init) = runFakeKeyCeremony(group, workingDir, workingDir, trusteeDir, nguardians, quorum, false)

        // encrypt
        group.showAndClearCountPowP()
        batchEncryption(group, workingDir, workingDir, inputBallotDir, invalidDir, "device11", nthreads, name1)
        //println("----------- after encrypt ${group.showAndClearCountPowP()}")

        // encrypt again, simulating the CAKE workflow of scanning the paper ballots
        val scannedBallotDir = "$workingDir/scan"
        batchEncryption(group, workingDir, scannedBallotDir, inputBallotDir, invalidDir, "scanPaper", nthreads, name1)
        //println("----------- after encrypt ${group.showAndClearCountPowP()}")

        val decryptingTrustees = readDecryptingTrustees(group, trusteeDir, init, present, true)

        group.showAndClearCountPowP()

        val pepBallotDir = "$workingDir/pep"
        println("runTrustedPep n = $quorum / $nguardians")
        RunTrustedPep.batchTrustedPep(
            group,
            workingDir,
            "$scannedBallotDir/encrypted_ballots/scanPaper",
            pepBallotDir,
            decryptingTrustees,
            nthreads
        )

        val nballots = 33
        val nencyptions = 100
        val nb = 3
        val expect = (8 + 8 * nguardians + 8 * nb) * nencyptions  * nballots // counting the verifier
        println("----------- after compareBallotPepEquivilence ${group.showAndClearCountPowP()}, expect=$expect")
    }
}