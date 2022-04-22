package electionguard.publish

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ElectionRecordTest {
    @Test
    fun readElectionRecordWrittenByDecryptorJava() {
        runTest {
            readElectionRecordAndValidate("src/commonTest/data/testJava/decryptor/")
        }
    }

    @Test
    fun readElectionRecordWrittenByDecryptorKotlin() {
        runTest {
            readElectionRecordAndValidate("src/commonTest/data/workflow/runDecryptingMediator")
        }
    }

    fun readElectionRecordAndValidate(topdir : String) {
        runTest {
            val group = productionGroup()
            val electionRecordIn = ElectionRecord(topdir, group)
            assertNotNull(electionRecordIn)
            val decryption = electionRecordIn.readDecryptionResult().getOrThrow { IllegalStateException(topdir) }
            readElectionRecord(decryption)
            validateTally(group, decryption.decryptedTally, decryption.availableGuardians.size)
        }
    }

    fun readElectionRecord(decryption: DecryptionResult) {
        val tallyResult = decryption.tallyResult
        val init = tallyResult.electionIntialized
        val config = init.config

        assertEquals("1.0.0", config.protoVersion)
        assertEquals("", config.constants.name)
        assertEquals("v0.95", config.manifest.specVersion)
        assertEquals(3, config.numberOfGuardians)
        assertEquals(2, config.quorum)
        assertEquals(3, init.guardians.size)
        assertEquals("accumulateTally", tallyResult.ciphertextTally.tallyId)
        assertEquals("accumulateTally", decryption.decryptedTally.tallyId)
        assertNotNull(decryption.decryptedTally)
        val contests = decryption.decryptedTally.contests
        assertNotNull(contests)
        val contest = contests["justice-supreme-court"]
        assertNotNull(contest)
        assertEquals(2, decryption.availableGuardians.size)
    }

    fun validateTally(group: GroupContext, tally: PlaintextTally, nguardians: Int?) {
        for (contest in tally.contests.values) {
            for (selection in contest.selections.values) {
                val actual : Int? = group.dLogG(selection.value) // LOOK should be K?
                assertEquals(selection.tally, actual)
                assertEquals(nguardians, selection.shares.size)
            }
        }
    }
}