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
    fun testDistPep() {
        val inputDir = "src/commonTest/data/workflow/allAvailableProto"
        val trusteeDir = "$inputDir/private_data/trustees"
        println("testDistPep with input= $inputDir\n   trustees= $trusteeDir")
        runDistPep(
            group,
            inputDir,
            RunTrustedTallyDecryption.readDecryptingTrustees(group, inputDir, trusteeDir),
        )
    }

    fun runDistPep(
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
        println("runDistPep present = $trusteeNames missing = $missingGuardians")

        val guardians = Guardians(group, electionInit.guardians)
        val plaintextEquivalenceProof = PlaintextEquivalenceProof(
            group,
            electionInit.extendedBaseHash,
            electionInit.jointPublicKey(),
            guardians,
            decryptingTrustees,
        )

        /*
        var count = 0
        for (eballot in electionRecord.encryptedAllBallots { true} ) {
            // println( eballot.show() )
            val result = distPepPrep.makeProof(eballot, eballot)
            assert (result is Ok)
            // println( result.unwrap() )
            count++
            break
        }

        val took = getSystemTimeInMillis() - starting
        println("runDistPep took $took millisecs for $count ballots")
         */

        println("---Same")
        val eballots1 = electionRecord.encryptedAllBallots { true}.iterator()
        val same =  eballots1.next()
        val resultSame = plaintextEquivalenceProof.makeProof(same, same)
        if (resultSame is Ok) {
            assertTrue(testResult(resultSame.unwrap()))
        } else {
            println (resultSame)
        }
        println()

        println("---Different")
        val eballots = electionRecord.encryptedAllBallots { true}.iterator()
        val eballot1 =  eballots.next()
        val eballot2 =  eballots.next()
        val result = plaintextEquivalenceProof.makeProof(eballot1, eballot2)
        if (result is Ok) {
            assertFalse(testResult(result.unwrap()))
        }  else {
            println (result)
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