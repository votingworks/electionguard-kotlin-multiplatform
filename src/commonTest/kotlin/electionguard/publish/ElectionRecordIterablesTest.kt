package electionguard.publish

import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.core.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import mu.KotlinLogging

private val logger = KotlinLogging.logger("ElectionRecordIterablesTest")

class ElectionRecordIterablesTest {
    val encryptorDir = "src/commonTest/data/testJava/decryptor/"
    val decryptorDir = "src/commonTest/data/testJava/decryptor/"

    @Test
    fun readBallotTalliesWrittenByDecryptorJava() {
        runTest {
            val context = productionGroup()
            readSpoiledBallotTallies(context, decryptorDir)
        }
    }

    @Test
    fun readBallotsWrittenByDecryptorJava() {
        runTest {
            val context = productionGroup()
            readBallots(context, decryptorDir)
            readCastBallots(context, decryptorDir)
            readSpoiledBallots(context, decryptorDir)
        }
    }

    @Test
    fun readBallotsWrittenByEncryptorJava() {
        runTest {
            val context = productionGroup()
            readBallots(context, encryptorDir)
            readCastBallots(context, encryptorDir)
            readSpoiledBallots(context, encryptorDir)
        }
    }

    fun readBallots(context: GroupContext, topdir: String) {
        val consumer = Consumer(topdir, context)
        val iterator = consumer.iterateSubmittedBallots().iterator()
        var count = 0;
        for (ballot in iterator) {
            logger.debug { "  $count readBallots ${ballot.ballotId} ${ballot.state}" }
            count++
        }
        assertEquals(count, 11)
    }

    fun readCastBallots(context: GroupContext, topdir: String) {
        val consumer = Consumer(topdir, context)
        val iterator = consumer.iterateCastBallots().iterator()
        var count = 0;
        for (ballot in iterator) {
            logger.debug { "  $count readCastBallots ${ballot.ballotId} ${ballot.state}" }
            count++
        }
        assertEquals(count, 5)
    }

    fun readSpoiledBallots(context: GroupContext, topdir: String) {
        val consumer = Consumer(topdir, context)
        val iterator = consumer.iterateSpoiledBallots().iterator()
        var count = 0;
        for (ballot in iterator) {
            logger.debug { "  $count readSpoiledBallots ${ballot.ballotId} ${ballot.state}" }
            count++
        }
        assertEquals(count, 6)
    }

    fun readSpoiledBallotTallies(context: GroupContext, topdir: String) {
        val consumer = Consumer(topdir, context)
        val iterator = consumer.iterateSpoiledBallotTallies().iterator()
        var count = 0;
        for (tally in iterator) {
            logger.debug { "  $count readSpoiledBallotTallies ${tally.tallyId}" }
            count++
        }
        assertEquals(count, 0)
    }
}