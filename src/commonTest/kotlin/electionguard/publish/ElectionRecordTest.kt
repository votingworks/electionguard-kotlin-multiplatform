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
            val consumerIn = Consumer(topdir, group)
            assertNotNull(consumerIn)
            val decryption = consumerIn.readDecryptionResult().getOrThrow { IllegalStateException(it) }
            readElectionRecord(decryption)
            validateTally(decryption.tallyResult.jointPublicKey(), decryption.decryptedTallyOrBallot)
        }
    }

    fun readElectionRecord(decryption: DecryptionResult) {
        val tallyResult = decryption.tallyResult
        val init = tallyResult.electionInitialized
        val config = init.config

        assertEquals("2.0.0", config.protoVersion)
        assertEquals("Standard", config.constants.name)
        assertEquals("v0.95", config.manifest.specVersion)
        assertEquals("RunWorkflow", tallyResult.encryptedTally.tallyId)
        assertEquals("RunWorkflow", decryption.decryptedTallyOrBallot.id)
        assertNotNull(decryption.decryptedTallyOrBallot)
        val contests = decryption.decryptedTallyOrBallot.contests
        assertNotNull(contests)
        val contest = contests["contest24"]
        assertNotNull(contest)

        assertEquals(init.guardians.size, config.numberOfGuardians)
        assertEquals(init.guardians.size, tallyResult.numberOfGuardians())

        assertEquals(init.guardians.size, config.numberOfGuardians)
        assertTrue(tallyResult.quorum() <= decryption.lagrangeCoordinates.size)
        assertTrue(tallyResult.numberOfGuardians() >= decryption.lagrangeCoordinates.size)
    }

    fun validateTally(jointKey: ElGamalPublicKey, tally: DecryptedTallyOrBallot) {
        for (contest in tally.contests.values) {
            for (selection in contest.selections.values) {
                val actual : Int? = jointKey.dLog(selection.value, 100)
                assertEquals(selection.tally, actual)
            }
        }
    }
}