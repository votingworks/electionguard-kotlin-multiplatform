package electionguard.publish

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ElectionRecordTest {
    // @Test
    fun readElectionRecordWrittenByDecryptorJava() {
        runTest {
            readElectionRecordAndValidate("src/commonTest/data/testJava/decryptor/")
        }
    }

    @Test
    fun readElectionRecordWrittenByDecryptorKotlin() {
        runTest {
            readElectionRecordAndValidate("src/commonTest/data/runWorkflow")
        }
    }

    fun readElectionRecordAndValidate(topdir : String) {
        runTest {
            val group = productionGroup()
            val electionRecordIn = ElectionRecord(topdir, group)
            assertNotNull(electionRecordIn)
            val decryption = electionRecordIn.readDecryptionResult().getOrThrow { IllegalStateException(it) }
            readElectionRecord(decryption)
            validateTally(decryption.tallyResult.jointPublicKey(), decryption.decryptedTally, decryption.availableGuardians.size)
        }
    }

    fun readElectionRecord(decryption: DecryptionResult) {
        val tallyResult = decryption.tallyResult
        val init = tallyResult.electionIntialized
        val config = init.config

        assertEquals("2.0.0", config.protoVersion)
        assertEquals("Standard", config.constants.name)
        assertEquals("v0.95", config.manifest.specVersion)
        assertEquals(3, config.numberOfGuardians)
        assertEquals(3, config.quorum)
        assertEquals(3, init.guardians.size)
        assertEquals("RunWorkflow", tallyResult.ciphertextTally.tallyId)
        assertEquals("RunWorkflow", decryption.decryptedTally.tallyId)
        assertNotNull(decryption.decryptedTally)
        val contests = decryption.decryptedTally.contests
        assertNotNull(contests)
        val contest = contests["contest24"]
        assertNotNull(contest)
        assertEquals(3, decryption.availableGuardians.size)
    }

    fun validateTally(jointKey: ElGamalPublicKey, tally: PlaintextTally, nguardians: Int?) {
        for (contest in tally.contests.values) {
            for (selection in contest.selections.values) {
                val actual : Int? = jointKey.dLog(selection.value, 100)
                assertEquals(selection.tally, actual)
                assertEquals(nguardians, selection.partialDecryptions.size)
            }
        }
    }
}