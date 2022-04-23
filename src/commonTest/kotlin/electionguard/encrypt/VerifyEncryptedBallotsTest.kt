package electionguard.encrypt

import electionguard.ballot.SubmittedBallot
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.publish.ElectionRecord
import electionguard.verifier.Verifier
import kotlin.test.Test
import kotlin.test.assertTrue

class VerifyEncryptedBallotsTest {
    val topdir = "src/commonTest/data/workflow"
    val ballotsJvmDir = "src/commonTest/data/testOut/jvm/runBatchEncryption"
    val ballotsNativeDir = "src/commonTest/data/testOut/native/runBatchEncryption"
    val ballotsPrecomputeDir = "src/commonTest/data/testOut/jvm/ballotPrecomputeTest/"

    // @Test
    fun verifyPrecomputeEncryptedBallots() {
        runTest {
            val group = productionGroup()
            val electionRecordIn = ElectionRecord(topdir, group)
            val verifier = Verifier(group, electionRecordIn)
            val ballots = readBallotsOne(group, ballotsPrecomputeDir)
            val ok = verifier.verifySubmittedBallots(ballots)
            assertTrue(ok)
        }
    }

    // LOOK fix this
    // @Test
    fun verifyJvmEncryptedBallots() {
        runTest {
            val group = productionGroup()
            val electionRecordIn = ElectionRecord(topdir, group)
            val verifier = Verifier(group, electionRecordIn)
            val ballots = readBallotsOne(group, ballotsJvmDir)
            val ok = verifier.verifySubmittedBallots(ballots)
            assertTrue(ok)
        }
    }

    // @Test
    fun verifyNativeEncryptedBallots() {
        runTest {
            val group = productionGroup()
            val electionRecordIn = ElectionRecord(topdir, group)
            val verifier = Verifier(group, electionRecordIn)
            val ballots = readBallotsOne(group, ballotsNativeDir)
            val ok = verifier.verifySubmittedBallots(ballots)
            assertTrue(ok)
        }
    }

    // this is slow - just do first one
    fun readBallotsOne(context: GroupContext, topdir: String): List<SubmittedBallot> {
        val electionRecordIn = ElectionRecord(topdir, context)
        val ballotIter =  electionRecordIn.iterateSubmittedBallots().iterator()
        assertTrue(ballotIter.hasNext())
        return listOf(ballotIter.next())
    }

    fun readBallots(context: GroupContext, topdir: String): List<SubmittedBallot> {
        val electionRecordIn = ElectionRecord(topdir, context)
        return electionRecordIn.iterateSubmittedBallots().toList()
    }

}