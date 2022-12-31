package electionguard.publish

import electionguard.core.GroupContext
import electionguard.core.productionGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import mu.KotlinLogging

private val logger = KotlinLogging.logger("ElectionRecordIterablesTest")

class ElectionRecordIterablesTest {
    // private val kotlinDir = "src/commonTest/data/runWorkflowSomeAvailable"
    private val kotlinDir = "testOut/RunElectionRecordConvertProto"

    @Test
    fun readBallotsWrittenByKotlin() {
        println("readBallotsWrittenByKotlin $kotlinDir")
        val context = productionGroup()
            readBallots(context, kotlinDir, 25)
            readCastBallots(context, kotlinDir, 25)
            readSpoiledBallots(context, kotlinDir, 0)
            readSpoiledBallotTallies(context, kotlinDir, 3)
    }

    fun readBallots(context: GroupContext, topdir: String, expected: Int) {
        println("readBallots $topdir")
        val consumerIn = makeConsumer(topdir, context)
        val iterator = consumerIn.iterateEncryptedBallots { true } .iterator()
        var count = 0
        for (ballot in iterator) {
            println("  $count iterateEncryptedBallots ${ballot.ballotId} ${ballot.state}")
            count++
        }
        assertEquals(expected, count)
    }

    fun readCastBallots(context: GroupContext, topdir: String, expected: Int) {
        val consumerIn = makeConsumer(topdir, context)
        val iterator = consumerIn.iterateCastBallots().iterator()
        var count = 0
        for (ballot in iterator) {
            logger.debug { "  $count readCastBallots ${ballot.ballotId} ${ballot.state}" }
            count++
        }
        assertEquals(expected, count)
    }

    fun readSpoiledBallots(context: GroupContext, topdir: String, expected: Int) {
        val consumerIn = makeConsumer(topdir, context)
        val iterator = consumerIn.iterateSpoiledBallots().iterator()
        var count = 0
        for (ballot in iterator) {
            logger.debug { "  $count readSpoiledBallots ${ballot.ballotId} ${ballot.state}" }
            count++
        }
        assertEquals(expected, count)
    }

    fun readSpoiledBallotTallies(context: GroupContext, topdir: String, expected: Int) {
        val consumerIn = makeConsumer(topdir, context)
        val iterator = consumerIn.iterateDecryptedBallots().iterator()
        var count = 0
        for (tally in iterator) {
            logger.debug { "  $count readSpoiledBallotTallies ${tally.id}" }
            count++
        }
        assertEquals(expected, count)
    }
}