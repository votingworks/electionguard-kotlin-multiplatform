package electionguard.publish

import com.github.michaelbull.result.*
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.decrypt.DecryptingTrusteeIF
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
            val electionRecordIn = ElectionRecord(topdir, context)
            val config = electionRecordIn.readElectionConfig().getOrThrow { IllegalStateException(topdir) }
            println("electionRecord.protoVersion = ${config.protoVersion}")
            assertEquals(config.protoVersion, "1.0.0")
            println("electionRecord.manifest.specVersion = ${config.manifest.specVersion}")
            assertEquals(config.manifest.electionScopeId, "jefferson-county-primary")
            assertEquals(config.manifest.specVersion, "v0.95")
        }
    }

    // TODO add SpoiledBallotTallies to test data
    // @Test
    fun readSpoiledBallotTallys() {
        runTest {
            val context = productionGroup()
            val electionRecordIn = ElectionRecord(topdir, context)
            var count = 0
            for (tally in electionRecordIn.iterateSpoiledBallotTallies()) {
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
            val electionRecordIn = ElectionRecord(topdir, context)
            var count = 0
            for (ballot in electionRecordIn.iterateSubmittedBallots()) {
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
            val electionRecordIn = ElectionRecord(topdir, context)
            var count = 0
            for (ballot in electionRecordIn.iterateCastBallots()) {
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
            val electionRecordIn = ElectionRecord(topdir, context)
            var count = 0
            for (ballot in electionRecordIn.iterateSpoiledBallots()) {
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
            val electionRecordIn = ElectionRecord(trusteeDir, context)
            var count = 0
            for (trustee: DecryptingTrusteeIF in electionRecordIn.readTrustees(trusteeDir)) {
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
            val electionRecordIn = ElectionRecord(trusteeDir, context)
            val result: Result<List<DecryptingTrusteeIF>, Throwable> = runCatching {
                electionRecordIn.readTrustees(trusteeDir)
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
                val electionRecordIn = ElectionRecord(trusteeDir, context)
                electionRecordIn.readTrustees(trusteeDir)
            }
            assertFalse(result is Ok)
        }
    }

    /*
    @Test
    fun readMissingElectionRecord() {
        runTest {
            val context = productionGroup()
            val electionDir = "src/commonTest/data/testJava/encryptor/election_record"
            val result: Result<ElectionRecord, Throwable> = runCatching {
                val electionRecordIn = ElectionRecord(electionDir, context)
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
            val electionRecordIn = ElectionRecord(electionDir, context)
            val result: Result<ElectionRecord, Throwable> = runCatching {
                val electionRecordIn = ElectionRecord(electionDir, context)
            }
            assertTrue(result is Err)
            assertTrue(result.getError() is pbandk.InvalidProtocolBufferException)
            println("readBadElectionRecord = ${result.getError()}")
            val message: String = result.getError()?.message ?: "not"
            assertTrue(message.contains("Unrecognized wire type: WireType(value=4)"))
        }
    }

    */
     */

}