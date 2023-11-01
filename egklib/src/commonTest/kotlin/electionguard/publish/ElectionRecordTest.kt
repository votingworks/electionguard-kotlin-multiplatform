package electionguard.publish

import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.unwrap
import electionguard.ballot.*
import electionguard.cli.RunVerifier
import electionguard.core.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Test election records that have been fully decrypted
class ElectionRecordTest {
    @Test
    fun allAvailableProto() {
        readElectionRecordAndValidate("src/commonTest/data/workflow/allAvailableProto")
    }

    @Test
    fun someAvailableProto() {
        readElectionRecordAndValidate("src/commonTest/data/workflow/someAvailableProto")
    }

    @Test
    fun allAvailableJson() {
        readElectionRecordAndValidate("src/commonTest/data/workflow/allAvailableJson")
    }

    @Test
    fun someAvailableJson() {
        readElectionRecordAndValidate("src/commonTest/data/workflow/someAvailableJson")
    }

    @Test
    fun chainedProto() {
        readElectionRecordAndValidate("src/commonTest/data/workflow/chainedProto")
    }

    @Test
    fun chainedJson() {
        readElectionRecordAndValidate("src/commonTest/data/workflow/chainedJson")
    }

    fun readElectionRecordAndValidate(topdir: String) {
        val group = productionGroup()
        val consumerIn = makeConsumer(group, topdir)
        assertNotNull(consumerIn)
        val decryption = consumerIn.readDecryptionResult().unwrap()
        readDecryption(decryption)
        validateTally(decryption.tallyResult.jointPublicKey(), decryption.decryptedTally)

        assertTrue(RunVerifier.runVerifier(group, topdir, 11, true))
    }

    fun readDecryption(decryption: DecryptionResult) {
        val tallyResult = decryption.tallyResult
        val init = tallyResult.electionInitialized
        val config = init.config

        assertEquals("production group, low memory use, 4096 bits", config.constants.name)
        // TODO assertEquals(specVersion, config.manifest.specVersion)
        assertNotNull(decryption.decryptedTally)
        val contests = decryption.decryptedTally.contests
        assertNotNull(contests)
        val contest = contests.find { it.contestId == "contest19" }
        assertNotNull(contest)

        assertEquals(init.guardians.size, config.numberOfGuardians)
        assertEquals(init.guardians.size, tallyResult.numberOfGuardians())

        assertEquals(init.guardians.size, config.numberOfGuardians)
    }

    fun validateTally(jointKey: ElGamalPublicKey, tally: DecryptedTallyOrBallot) {
        for (contest in tally.contests) {
            for (selection in contest.selections) {
                val actual: Int? = jointKey.dLog(selection.bOverM, 100)
                assertEquals(selection.tally, actual)
            }
        }
    }
}