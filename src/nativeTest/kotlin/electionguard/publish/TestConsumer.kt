package electionguard.publish

import electionguard.core.productionGroup
import electionguard.core.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestConsumer {
    private val topdir = "src/commonTest/data/workflow/decryptor/"

    @Test
    fun readElectionRecordNative() {
        runTest {
            val context = productionGroup()
            val consumer = Consumer( topdir, context)
            val electionRecord = consumer.readElectionRecord()
            println("electionRecord.protoVersion = ${electionRecord.protoVersion}")
            assertEquals(electionRecord.protoVersion, "1.0.0")
            println("electionRecord.manifest.specVersion = ${electionRecord.manifest.specVersion}")
            assertEquals(electionRecord.manifest.electionScopeId, "jefferson-county-primary")
            assertEquals(electionRecord.manifest.specVersion, "v0.95")
        }
    }

    @Test
    fun readElectionRecordAllNative() {
        runTest {
            val context = productionGroup()
            val consumer = Consumer( topdir, context)
            val electionRecord = consumer.readElectionRecordAllData()
            println("electionRecord.protoVersion = ${electionRecord.protoVersion}")
            assertEquals(electionRecord.protoVersion, "1.0.0")
            println("electionRecord.manifest.specVersion = ${electionRecord.manifest.specVersion}")
            assertEquals(electionRecord.manifest.electionScopeId, "jefferson-county-primary")
            assertEquals(electionRecord.manifest.specVersion, "v0.95")
        }
    }

    @Test
    fun readSpoiledBallotTallysNative() {
        runTest {
            val context = productionGroup()
            val consumer = Consumer( topdir, context)
            var count = 0
            for (tally in consumer.iterateSpoiledBallotTallies()) {
                println("$count tally = ${tally.tallyId}")
                assertTrue(tally.tallyId.startsWith("ballot-id"))
                count++
            }
        }
    }

    @Test
    fun readSubmittedBallotsNative() {
        runTest {
            val context = productionGroup()
            val consumer = Consumer( topdir, context)
            var count = 0
            for (ballot in consumer.iterateSubmittedBallots()) {
                println("$count ballot = ${ballot.ballotId}")
                assertTrue(ballot.ballotId.startsWith("ballot-id"))
                count++
            }
        }
    }

    @Test
    fun readSubmittedBallotsCastNative() {
        runTest {
            val context = productionGroup()
            val consumer = Consumer( topdir, context)
            var count = 0
            for (ballot in consumer.iterateCastBallots()) {
                println("$count ballot = ${ballot.ballotId}")
                assertTrue(ballot.ballotId.startsWith("ballot-id"))
                count++
            }
        }
    }

    @Test
    fun readSubmittedBallotsSpoiledNative() {
        runTest {
            val context = productionGroup()
            val consumer = Consumer( topdir, context)
            var count = 0
            for (ballot in consumer.iterateSpoiledBallots()) {
                println("$count ballot = ${ballot.ballotId}")
                assertTrue(ballot.ballotId.startsWith("ballot-id"))
                count++
            }
        }
    }

}