package electionguard.encrypt

import electionguard.ballot.SubmittedBallot
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.publish.Consumer
import electionguard.verifier.Verifier
import kotlin.test.Test
import kotlin.test.assertTrue

class VerifyEncryptedBallotsTest {
    val topdir = "src/commonTest/data/testJava/kickstart/encryptor"
    val ballotsJvmDir = "src/commonTest/data/testOut/jvm/runBatchEncryption"
    val ballotsNativeDir = "src/commonTest/data/testOut/native/runBatchEncryption"
    val ballotsPrecomputeDir = "src/commonTest/data/testOut/jvm/ballotPrecomputeTest/"

    @Test
    fun verifyPrecomputeEncryptedBallots() {
        runTest {
            val group = productionGroup()
            val consumer = Consumer(topdir, group)
            val electionRecord = consumer.readElectionRecord()
            val verifier = Verifier(group, electionRecord)
            val ballots = readBallots(group, ballotsPrecomputeDir)
            val ok = verifier.verifySubmittedBallots(ballots)
            assertTrue(ok)
        }
    }

    @Test
    fun verifyJvmEncryptedBallots() {
        runTest {
            val group = productionGroup()
            val consumer = Consumer(topdir, group)
            val electionRecord = consumer.readElectionRecord()
            val verifier = Verifier(group, electionRecord)
            val ballots = readBallots(group, ballotsJvmDir)
            val ok = verifier.verifySubmittedBallots(ballots)
            assertTrue(ok)
        }
    }

    @Test
    fun verifyNativeEncryptedBallots() {
        runTest {
            val group = productionGroup()
            val consumer = Consumer(topdir, group)
            val electionRecord = consumer.readElectionRecord()
            val verifier = Verifier(group, electionRecord)
            val ballots = readBallots(group, ballotsNativeDir)
            val ok = verifier.verifySubmittedBallots(ballots)
            assertTrue(ok)
        }
    }

    fun readBallots(context: GroupContext, topdir: String): List<SubmittedBallot> {
        val consumer = Consumer(topdir, context)
        return consumer.iterateSubmittedBallots().toList()
    }

}