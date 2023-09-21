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

class PlaintextEquivalenceProofTest {
    @Test
    fun testDistPep() {
        val group = productionGroup()
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
        val result1 = plaintextEquivalenceProof.makeProof(same, same)
        assert (result1 is Ok)
        println( result1.unwrap().show() )
        val sum1 = testResult(result1.unwrap(), true)
        println()

        println("---Different")
        val eballots = electionRecord.encryptedAllBallots { true}.iterator()
        val eballot1 =  eballots.next()
        val eballot2 =  eballots.next()
        val result = plaintextEquivalenceProof.makeProof(eballot1, eballot2)
        assert (result is Ok)
        println( result.unwrap().show() )
        val sum2 = testResult(result1.unwrap(), false)
        assertEquals(sum1, sum2)
    }

    fun testResult(result: DecryptedTallyOrBallot, isEqual: Boolean): Int {
        var sum = 0
        result.contests.forEach {
            it.selections.forEach {
                sum += it.tally
                if (isEqual && it.tally != 10) {
                    println("  HEY NOT EQUAL")
                }
            }
        }
        return sum
    }
}