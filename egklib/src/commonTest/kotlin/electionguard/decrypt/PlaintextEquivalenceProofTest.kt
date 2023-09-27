package electionguard.decrypt

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.cli.RunTrustedTallyDecryption
import electionguard.core.GroupContext
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.publish.readElectionRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaintextEquivalenceProofTest {
    val group = productionGroup()

    @Test
    fun testEgkPep() {
        val inputDir = "src/commonTest/data/workflow/allAvailableProto"
        val trusteeDir = "$inputDir/private_data/trustees"
        println("=== testDistPep with input= $inputDir\n   trustees= $trusteeDir")
        runEgkPep(
            group,
            inputDir,
            RunTrustedTallyDecryption.readDecryptingTrustees(group, inputDir, trusteeDir),
        )
    }

    fun runEgkPep(
        group: GroupContext,
        inputDir: String,
        decryptingTrustees: List<DecryptingTrusteeIF>,
    ) {
        val starting = getSystemTimeInMillis()

        val electionRecord = readElectionRecord(group, inputDir)
        val electionInit = electionRecord.electionInit()!!

        val trusteeNames = decryptingTrustees.map { it.id() }.toSet()
        val missingGuardians =
            electionInit.guardians.filter { !trusteeNames.contains(it.guardianId) }.map { it.guardianId }
        println("   present = $trusteeNames missing = $missingGuardians")

        val guardians = Guardians(group, electionInit.guardians)
        val egkPep = PlaintextEquivalenceProof(
            group,
            electionInit.extendedBaseHash,
            electionInit.jointPublicKey(),
            guardians,
            decryptingTrustees,
        )

        var count = 0
        for (eballot in electionRecord.encryptedAllBallots { true} ) {
            val result = egkPep.testEquivalent(eballot, eballot)
            assert (result is Ok)
            assertTrue (result.unwrap())
            println(" same ballot ${eballot.ballotId}")
            count++
        }

        val took = getSystemTimeInMillis() - starting
        val per = took / (count * 1000.0)
        println("runDistPep took $took millisecs for $count ballots = ${"%.3f".format(per)} secs/ballot")
        println()

        val eballots = electionRecord.encryptedAllBallots { true}.iterator()
        val same =  eballots.next()
        val resultSame = egkPep.doEgkPep(same, same)
        if (resultSame is Ok) {
            assertTrue(testResult(resultSame.unwrap()))
            println(" same ballot ${same.ballotId}")
        } else {
            println (resultSame)
        }

        var eballot1 =  eballots.next()
        var eballot2 =  eballots.next()
        val result = egkPep.doEgkPep(eballot1, eballot2)
        if (result is Ok) {
            assertFalse(testResult(result.unwrap()))
            println(" different ballot ${eballot1.ballotId}, ${eballot2.ballotId}")
        }  else {
            println (result)
        }

        val eballot3 =  eballots.next()
        val eballot4 =  eballots.next()
        val result2 = egkPep.testEquivalent(eballot3, eballot4)
        if (result2 is Ok) {
            assertFalse(result2.unwrap())
            println(" different ballot ${eballot3.ballotId}, ${eballot4.ballotId}")
        }  else {
            println (result2)
        }
    }

    fun testResult(result: DecryptedTallyOrBallot): Boolean {
        var allEqual = true
        result.contests.forEach {
            it.selections.forEach {
                allEqual = allEqual && it.bOverM.equals(group.ONE_MOD_P)
            }
        }
        return allEqual
    }
}