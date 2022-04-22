package electionguard.encrypt

import electionguard.ballot.SubmittedBallot
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.publish.ElectionRecord
import kotlin.test.Test
import kotlin.test.assertEquals

class CompareEncryptedBallotsTest {
    val topdir = "src/commonTest/data/testJava/kickstart/encryptor"
    val ballotsJvmDir = "src/commonTest/data/testOut/jvm/runBatchEncryption"
    val ballotsNativeDir = "src/commonTest/data/testOut/native/runBatchEncryption"
    val ballotsPrecomputeDir = "src/commonTest/data/testOut/jvm/ballotPrecomputeTest"
    @Test
    fun compareWithPrecomputed() {
        runTest {
            val context = productionGroup()
            val ballotsJvm = readBallots(context, ballotsJvmDir)
            val ballotsPrecomputed = readBallots(context, ballotsPrecomputeDir).associateBy { it.ballotId }
            ballotsJvm.forEach { ballot1 ->
                val ballot2 = ballotsPrecomputed[ballot1.ballotId] ?: throw IllegalStateException("no ballot ${ballot1.ballotId}")

                ballot1.contests.forEachIndexed { indexc, contest1 ->
                    val contest2 = ballot2.contests[indexc]

                    contest1.selections.forEachIndexed { indexs, selection1 ->
                        val selection2 = contest2.selections[indexs]
                        assertEquals(selection1, selection2)
                    }
                    assertEquals(contest1, contest2)
                }

                assertEquals(ballot1, ballot2)
                println("ok ${ballot1.ballotId}")
            }
        }
    }

    @Test
    fun compareJvmAndNative() {
        runTest {
            val context = productionGroup()
            val ballotsJvm = readBallots(context, ballotsJvmDir)
            val ballotsNative = readBallots(context, ballotsNativeDir).associateBy { it.ballotId }
            ballotsJvm.forEach { ballot1 ->
                val ballot2 = ballotsNative[ballot1.ballotId] ?: throw IllegalStateException("no ballot ${ballot1.ballotId}")
                assertEquals(ballot1, ballot2)
                println("ok ${ballot1.ballotId}")
            }
        }
    }

    fun readBallots(context: GroupContext, topdir: String): List<SubmittedBallot> {
        val electionRecordIn = ElectionRecord(topdir, context)
        return electionRecordIn.iterateSubmittedBallots().toList()
    }

}