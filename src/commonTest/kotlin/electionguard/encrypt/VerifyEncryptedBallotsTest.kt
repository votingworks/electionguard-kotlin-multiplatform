package electionguard.encrypt

import electionguard.ballot.EncryptedBallot
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.publish.Consumer
import electionguard.publish.electionRecordFromConsumer
import electionguard.verifier.Verifier
import kotlin.test.assertTrue

class VerifyEncryptedBallotsTest {
    val topdir = "src/commonTest/data/runWorkflowAllAvailable"
    val ballotsJvmDir = "src/commonTest/data/testOut/jvm/runBatchEncryption"
    val ballotsNativeDir = "src/commonTest/data/testOut/native/runBatchEncryption"
    val ballotsPrecomputeDir = "src/commonTest/data/testOut/jvm/ballotPrecomputeTest/"

    // @Test
    fun verifyPrecomputeEncryptedBallots() {
        runTest {
            val group = productionGroup()
            val electionRecord = electionRecordFromConsumer(Consumer(topdir, group))
            val verifier = Verifier(electionRecord)
            val ok = verifier.verifyEncryptedBallots()
            assertTrue(ok.allOk)
        }
    }

    // TODO fix this
    // @Test
    fun verifyJvmEncryptedBallots() {
        runTest {
            val group = productionGroup()
            val electionRecord = electionRecordFromConsumer(Consumer(topdir, group))
            val verifier = Verifier(electionRecord)
            val ok = verifier.verifyEncryptedBallots()
            assertTrue(ok.allOk)
        }
    }

    // @Test
    fun verifyNativeEncryptedBallots() {
        runTest {
            val group = productionGroup()
            val electionRecord = electionRecordFromConsumer(Consumer(topdir, group))
            val verifier = Verifier(electionRecord)
            val ok = verifier.verifyEncryptedBallots()
            assertTrue(ok.allOk)
        }
    }

    // this is slow - just do first one
    fun readBallotsOne(context: GroupContext, topdir: String): List<EncryptedBallot> {
        val consumerIn = Consumer(topdir, context)
        val ballotIter =  consumerIn.iterateEncryptedBallots{true}.iterator()
        assertTrue(ballotIter.hasNext())
        return listOf(ballotIter.next())
    }

    fun readBallots(context: GroupContext, topdir: String): List<EncryptedBallot> {
        val consumerIn = Consumer(topdir, context)
        return consumerIn.iterateEncryptedBallots { true} .toList()
    }

}