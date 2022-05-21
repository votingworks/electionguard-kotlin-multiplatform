package electionguard.publish

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Test election records that have been fully decrypted
class ElectionRecordTest {
    @Test
    fun readElectionRecordAllAvailable() {
        runTest {
            readElectionRecordAndValidate("src/commonTest/data/runWorkflowAllAvailable")
        }
    }

    @Test
    fun readElectionRecordSomeAvailable() {
        runTest {
            readElectionRecordAndValidate("src/commonTest/data/runWorkflowSomeAvailable")
        }
    }

    fun readElectionRecordAndValidate(topdir : String) {
        runTest {
            val group = productionGroup()
            val electionRecordIn = ElectionRecord(topdir, group)
            assertNotNull(electionRecordIn)
            val decryption = electionRecordIn.readDecryptionResult().getOrThrow { IllegalStateException(it) }
            readElectionRecord(decryption)
            validateTally(decryption.tallyResult.jointPublicKey(), decryption.decryptedTally,
                decryption.numberOfGuardians(), decryption.quorum(), decryption.availableGuardians.size)
        }
    }

    fun readElectionRecord(decryption: DecryptionResult) {
        val tallyResult = decryption.tallyResult
        val init = tallyResult.electionIntialized
        val config = init.config

        assertEquals("2.0.0", config.protoVersion)
        assertEquals("Standard", config.constants.name)
        assertEquals("v0.95", config.manifest.specVersion)
        assertEquals("RunWorkflow", tallyResult.encryptedTally.tallyId)
        assertEquals("RunWorkflow", decryption.decryptedTally.tallyId)
        assertNotNull(decryption.decryptedTally)
        val contests = decryption.decryptedTally.contests
        assertNotNull(contests)
        val contest = contests["contest24"]
        assertNotNull(contest)

        assertEquals(init.guardians.size, config.numberOfGuardians)
        assertEquals(init.guardians.size, tallyResult.numberOfGuardians())

        assertEquals(init.guardians.size, config.numberOfGuardians)
        assertTrue(tallyResult.quorum() <= decryption.availableGuardians.size)
        assertTrue(tallyResult.numberOfGuardians() >= decryption.availableGuardians.size)
    }

    fun validateTally(jointKey: ElGamalPublicKey, tally: PlaintextTally, nguardians: Int, quorum: Int, navailable: Int) {
        for (contest in tally.contests.values) {
            for (selection in contest.selections.values) {
                val actual : Int? = jointKey.dLog(selection.value, 100)
                assertEquals(selection.tally, actual)
                assertEquals(nguardians, selection.partialDecryptions.size)
                // directly computed == navailable
                assertEquals(navailable, selection.partialDecryptions.filter { it.proof != null}.count())
                // indirectly computed (aka compensated, recovered)  == nguardians - navailable
                assertEquals(nguardians - navailable, selection.partialDecryptions.filter { it.recoveredDecryptions.isNotEmpty()}.count())
                // the compensated decryptions have a quorum
                selection.partialDecryptions.filter { it.recoveredDecryptions.isNotEmpty()}.forEach {
                    assertEquals(quorum, it.recoveredDecryptions.count())
                }
            }
        }
    }
}