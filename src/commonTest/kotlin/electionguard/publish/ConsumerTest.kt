package electionguard.publish

import com.github.michaelbull.result.*
import electionguard.ballot.ElectionRecord
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.decrypt.DecryptingTrusteeIF
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConsumerTest {
    private val topdir = "src/commonTest/data/testJava/decryptor"

    @Test
    fun readElectionRecord() {
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
    fun readElectionRecordAll() {
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

    // TODO add SpoiledBallotTallies to test data
    // @Test
    fun readSpoiledBallotTallys() {
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
    fun readSubmittedBallots() {
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
    fun readSubmittedBallotsCast() {
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
    fun readSubmittedBallotsSpoiled() {
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

    @Test
    fun readTrustees() {
        runTest {
            val context = productionGroup()
            val trusteeDir = "src/commonTest/data/testJava/keyCeremony/election_private_data"
            val consumer = Consumer( trusteeDir, context)
            var count = 0
            for (trustee: DecryptingTrusteeIF in consumer.readTrustees(trusteeDir)) {
                println("$count trustee = ${trustee}")
                assertTrue(trustee.id().startsWith("remoteTrustee"))
                count++
            }
        }
    }

    @Test
    fun readBadTrustees() {
        runTest {
            val context = productionGroup()
            val trusteeDir = "src/commonTest/data/testJava/encryptor/election_record"
            val consumer = Consumer( trusteeDir, context)
            val result: Result<List<DecryptingTrusteeIF>, Throwable> = runCatching {
                consumer.readTrustees(trusteeDir)
            }
            assertTrue(result is Err)
            assertTrue(result.getError() is pbandk.InvalidProtocolBufferException)
            val message: String = result.getError()?.message ?: "not"
            assertTrue(message.contains("Protocol message contained an invalid tag"))
        }
    }

    @Test
    fun readMissingTrustees() {
        runTest {
            val context = productionGroup()
            val trusteeDir = "src/commonTest/data/testBad/nonexistant"
            val result: Result<List<DecryptingTrusteeIF>, Throwable> = runCatching {
                val consumer = Consumer( trusteeDir, context)
                consumer.readTrustees(trusteeDir)
            }
            assertFalse(result is Ok)
        }
    }

    @Test
    fun readMissingElectionRecord() {
        runTest {
            val context = productionGroup()
            val electionDir = "src/commonTest/data/testJava/encryptor/election_record"
            val consumer = Consumer( electionDir, context)
            val result: Result<ElectionRecord, Throwable> = runCatching {
                consumer.readElectionRecord()
            }
            assertTrue(result is Err)
            println("readMissingElectionRecord ${result.getError()}")
            val message: String = result.getError()?.message ?: "not"
            assertTrue(message.contains("No such file or directory"))
        }
    }

    @Test
    fun readBadElectionRecord() {
        runTest {
            val context = productionGroup()
            // the submittedBallots.protobuf was renamed to electionRecord.protobuf
            val electionDir = "src/commonTest/data/testBad"
            val consumer = Consumer( electionDir, context)
            val result: Result<ElectionRecord, Throwable> = runCatching {
                consumer.readElectionRecord()
            }
            assertTrue(result is Err)
            assertTrue(result.getError() is pbandk.InvalidProtocolBufferException)
            println("readBadElectionRecord = ${result.getError()}")
            val message: String = result.getError()?.message ?: "not"
            assertTrue(message.contains("Unrecognized wire type: WireType(value=4)"))
        }
    }

}