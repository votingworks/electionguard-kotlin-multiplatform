package electionguard.publish

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.*
import electionguard.input.specVersion
import electionguard.verifier.runVerifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Test election records that have been fully decrypted
class ElectionRecordTest {
    @Test
    fun readElectionRecordAllAvailable() {
        readElectionRecordAndValidate("src/commonTest/data/runWorkflowAllAvailable")
    }

    @Test
    fun readElectionRecordSomeAvailable() {
        readElectionRecordAndValidate("src/commonTest/data/runWorkflowSomeAvailable")
    }

    // @Test
    fun readElectionRecordJson() {
        readElectionRecordAndValidate("src/commonTest/data/testElectionRecord/convertJson")
    }

    // @Test
    fun readElectionRecordRoundtrip() {
        readElectionRecordAndValidate("src/commonTest/data/testElectionRecord/roundtripProto")
    }

    fun readElectionRecordAndValidate(topdir: String) {
        val group = productionGroup()
        val consumerIn = makeConsumer(topdir, group)
        assertNotNull(consumerIn)
        val decryption = consumerIn.readDecryptionResult().getOrThrow { IllegalStateException(it) }
        readDecryption(decryption)
        validateTally(decryption.tallyResult.jointPublicKey(), decryption.decryptedTally)

        //
        assertTrue(runVerifier(group, topdir, 11, true))
    }

    fun readDecryption(decryption: DecryptionResult) {
        val tallyResult = decryption.tallyResult
        val init = tallyResult.electionInitialized
        val config = init.config

        assertEquals("production group, low memory use, 4096 bits", config.constants.name)
        // TODO assertEquals(specVersion, config.manifest.specVersion)
        assertEquals("RunWorkflow", tallyResult.encryptedTally.tallyId)
        assertEquals("RunWorkflow", decryption.decryptedTally.id)
        assertNotNull(decryption.decryptedTally)
        val contests = decryption.decryptedTally.contests
        assertNotNull(contests)
        val contest = contests.find { it.contestId == "contest19" }
        assertNotNull(contest)

        assertEquals(init.guardians.size, config.numberOfGuardians)
        assertEquals(init.guardians.size, tallyResult.numberOfGuardians())

        assertEquals(init.guardians.size, config.numberOfGuardians)
        // assertTrue(tallyResult.quorum() <= decryption.lagrangeCoordinates.size)
        // assertTrue(tallyResult.numberOfGuardians() >= decryption.lagrangeCoordinates.size)
    }

    fun validateTally(jointKey: ElGamalPublicKey, tally: DecryptedTallyOrBallot) {
        for (contest in tally.contests) {
            for (selection in contest.selections) {
                val actual: Int? = jointKey.dLog(selection.kExpTally, 100)
                assertEquals(selection.tally, actual)
            }
        }
    }
}