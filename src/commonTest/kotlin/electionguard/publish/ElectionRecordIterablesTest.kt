package electionguard.publish

import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.core.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import mu.KotlinLogging

private val logger = KotlinLogging.logger("ElectionRecordIterablesTest")

class ElectionRecordIterablesTest {
    val kotlinDir = "src/commonTest/data/workflow"
    val decryptorDir = "src/commonTest/data/testJava/decryptor/"

    @Test
    fun readBallotsWrittenByKotlin() {
        runTest {
            val context = productionGroup()
            readBallots(context, kotlinDir, 11)
            readCastBallots(context, kotlinDir, 11)
            readSpoiledBallots(context, kotlinDir, 0)
        }
    }

    // @Test
    fun readBallotsWrittenByJava() {
        runTest {
            val context = productionGroup()
            readBallots(context, kotlinDir, 11)
            readCastBallots(context, kotlinDir, 6)
            readSpoiledBallots(context, kotlinDir, 5)
        }
    }

    fun readBallots(context: GroupContext, topdir: String, expected: Int) {
        val electionRecordIn = ElectionRecord(topdir, context)
        val iterator = electionRecordIn.iterateSubmittedBallots().iterator()
        var count = 0;
        for (ballot in iterator) {
            logger.debug { "  $count readBallots ${ballot.ballotId} ${ballot.state}" }
            count++
        }
        assertEquals(expected, count)
    }

    fun readCastBallots(context: GroupContext, topdir: String, expected: Int) {
        val electionRecordIn = ElectionRecord(topdir, context)
        val iterator = electionRecordIn.iterateCastBallots().iterator()
        var count = 0;
        for (ballot in iterator) {
            logger.debug { "  $count readCastBallots ${ballot.ballotId} ${ballot.state}" }
            count++
        }
        assertEquals(expected, count)
    }

    fun readSpoiledBallots(context: GroupContext, topdir: String, expected: Int) {
        val electionRecordIn = ElectionRecord(topdir, context)
        val iterator = electionRecordIn.iterateSpoiledBallots().iterator()
        var count = 0;
        for (ballot in iterator) {
            logger.debug { "  $count readSpoiledBallots ${ballot.ballotId} ${ballot.state}" }
            count++
        }
        assertEquals(expected, count)
    }

    fun readSpoiledBallotTallies(context: GroupContext, topdir: String, expected: Int) {
        val electionRecordIn = ElectionRecord(topdir, context)
        val iterator = electionRecordIn.iterateSpoiledBallotTallies().iterator()
        var count = 0;
        for (tally in iterator) {
            logger.debug { "  $count readSpoiledBallotTallies ${tally.tallyId}" }
            count++
        }
        assertEquals(expected, count)
    }
}