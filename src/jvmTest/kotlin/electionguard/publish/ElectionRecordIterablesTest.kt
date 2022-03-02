package electionguard.publish

import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.core.runTest
import publish.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals

class ElectionRecordIterablesTest {

    @Test
    fun readBallotTalliesWrittenByDecryptorJava() {
        runTest {
            val context = productionGroup()
             readSpoiledBallotTallies(context, "src/commonTest/data/workflow/decryptor/election_record/")
        }
    }

    @Test
    fun readBallotsWrittenByDecryptorJava() {
        runTest {
            val context = productionGroup()
            readBallots(context, "src/commonTest/data/workflow/decryptor/election_record/")
            readCastBallots(context, "src/commonTest/data/workflow/decryptor/election_record/")
            readSpoiledBallots(context, "src/commonTest/data/workflow/decryptor/election_record/")
        }
    }

    @Test
    fun readBallotsWrittenByEncryptorJava() {
        runTest {
            val context = productionGroup()
            readBallots(context, "src/commonTest/data/workflow/encryptor/election_record/")
            readCastBallots(context, "src/commonTest/data/workflow/encryptor/election_record/")
            readSpoiledBallots(context, "src/commonTest/data/workflow/encryptor/election_record/")
        }
    }

    fun readBallots(context: GroupContext, topdir: String) {
        val consumer: Consumer = Consumer.fromElectionRecord(topdir, context)
        val iterator = consumer.iterateSubmittedBallots().iterator()
        var count = 0;
        for (ballot in iterator) {
            System.out.printf("  %d ConsumerTest.readBallots %s %s%n", count++, ballot.ballotId, ballot.state)
        }
        assertEquals(count, 11)
    }

    fun readCastBallots(context: GroupContext, topdir: String) {
        val consumer: Consumer = Consumer.fromElectionRecord(topdir, context)
        val iterator = consumer.iterateCastBallots().iterator()
        var count = 0;
        for (ballot in iterator) {
            System.out.printf("  %d ConsumerTest.readCastBallots %s %s%n", count++, ballot.ballotId, ballot.state)
        }
        assertEquals(count, 5)
    }

    fun readSpoiledBallots(context: GroupContext, topdir: String) {
        val consumer: Consumer = Consumer.fromElectionRecord(topdir, context)
        val iterator = consumer.iterateSpoiledBallots().iterator()
        var count = 0;
        for (ballot in iterator) {
            System.out.printf("  %d ConsumerTest.readCastBallots %s %s%n", count++, ballot.ballotId, ballot.state)
        }
        assertEquals(count, 6)
    }

    fun readSpoiledBallotTallies(context: GroupContext, topdir: String) {
        val consumer: Consumer = Consumer.fromElectionRecord(topdir, context)
        val iterator = consumer.iterateSpoiledBallotTallies().iterator()
        var count = 0;
        for (tally in iterator) {
            System.out.printf("  %d ConsumerTest.readSpoiledBallotTallies %s%n", count++, tally.tallyId)
        }
        assertEquals(count, 6)
    }

}