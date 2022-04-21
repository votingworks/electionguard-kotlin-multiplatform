package electionguard.encrypt

import electionguard.ballot.SubmittedBallot
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.publish.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals

class CompareEncryptedBallotsTest {

    // remove tests until data is added to github
    // @Test
    fun compareJvmAndNative() {
        runTest {
            val context = productionGroup()
            val ballotsJvm = readBallots(context, "testOut/jvm/runBatchEncryption")
            val ballotsNative = readBallots(context, "testOut/native/runBatchEncryption")
            ballotsJvm.forEachIndexed { index, ballot1 ->
                val ballot2 = ballotsNative[index]
                assertEquals(ballot1, ballot2)
                println("ok ${ballot1.ballotId}")
            }
        }
    }

    fun readBallots(context: GroupContext, topdir: String): List<SubmittedBallot> {
        val consumer = Consumer(topdir, context)
        return consumer.iterateSubmittedBallots().toList()
    }

}