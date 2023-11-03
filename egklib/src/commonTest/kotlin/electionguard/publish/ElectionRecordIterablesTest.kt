package electionguard.publish

import electionguard.core.GroupContext
import electionguard.core.productionGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger("ElectionRecordIterablesTest")

class ElectionRecordIterablesTest {
    val context = productionGroup()

    @Test
    fun testElectionRecordIterablesProto() {
        readBallots("src/commonTest/data/workflow/chainedProto")
    }

    @Test
    fun testElectionRecordIterablesJson() {
        readBallots("src/commonTest/data/workflow/chainedJson")
    }

    fun readBallots(topdir: String) {
        println("readBallots $topdir")
        readBallots(context, topdir, 11)
        readCastBallots(context, topdir, 8)
        readSpoiledBallots(context, topdir, 3)
        readDecryptedBallots(context, topdir, 3) // when do we decrypt spoiled ballots ??
    }

    fun readBallots(context: GroupContext, topdir: String, expected: Int) {
        val consumerIn = makeConsumer(context, topdir)
        val iterator = consumerIn.iterateAllEncryptedBallots { true } .iterator()
        var count = 0
        for (ballot in iterator) {
            println("  $count iterateEncryptedBallots ${ballot.ballotId} ${ballot.state}")
            count++
        }
        assertEquals(expected, count)
    }

    fun readCastBallots(context: GroupContext, topdir: String, expected: Int) {
        val consumerIn = makeConsumer(context, topdir)
        val iterator = consumerIn.iterateAllCastBallots().iterator()
        var count = 0
        for (ballot in iterator) {
            logger.debug { "  $count readCastBallots ${ballot.ballotId} ${ballot.state}" }
            count++
        }
        assertEquals(expected, count)
    }

    fun readSpoiledBallots(context: GroupContext, topdir: String, expected: Int) {
        val consumerIn = makeConsumer(context, topdir)
        val iterator = consumerIn.iterateAllSpoiledBallots().iterator()
        var count = 0
        for (ballot in iterator) {
            logger.debug { "  $count readSpoiledBallots ${ballot.ballotId} ${ballot.state}" }
            count++
        }
        assertEquals(expected, count)
    }

    fun readDecryptedBallots(context: GroupContext, topdir: String, expected: Int) {
        val consumerIn = makeConsumer(context, topdir)
        val iterator = consumerIn.iterateDecryptedBallots().iterator()
        var count = 0
        for (tally in iterator) {
            logger.debug { "  $count readSpoiledBallotTallies ${tally.id}" }
            count++
        }
        assertEquals(expected, count)
    }
}