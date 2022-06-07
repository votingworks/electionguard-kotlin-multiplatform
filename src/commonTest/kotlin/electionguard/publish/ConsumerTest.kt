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
    private val topdir = "src/commonTest/data/runWorkflowAllAvailable"

    @Test
    fun readElectionRecord() {
        runTest {
            val context = productionGroup()
            val consumerIn = Consumer(topdir, context)
            val init = consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }
            val config = init.config
            println("electionRecord.protoVersion = ${config.protoVersion}")
            assertEquals("2.0.0", config.protoVersion)
            println("electionRecord.manifest.specVersion = ${config.manifest.specVersion}")
            assertEquals("election_scope_id", config.manifest.electionScopeId)
            assertEquals("v0.95", config.manifest.specVersion)
        }
    }

    @Test
    fun readSpoiledBallotTallys() {
        runTest {
            val context = productionGroup()
            val consumerIn = Consumer(topdir, context)
            var count = 0
            for (tally in consumerIn.iterateSpoiledBallotTallies()) {
                println("$count tally = ${tally.tallyId}")
                assertTrue(tally.tallyId.startsWith("ballot-id"))
                count++
            }
        }
    }

    @Test
    fun readEncryptedBallots() {
        runTest {
            val context = productionGroup()
            val consumerIn = Consumer(topdir, context)
            var count = 0
            for (ballot in consumerIn.iterateEncryptedBallots { true} ) {
                println("$count ballot = ${ballot.ballotId}")
                assertTrue(ballot.ballotId.startsWith("ballot-id"))
                count++
            }
        }
    }

    @Test
    fun readEncryptedBallotsCast() {
        runTest {
            val context = productionGroup()
            val consumerIn = Consumer(topdir, context)
            var count = 0
            for (ballot in consumerIn.iterateCastBallots()) {
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
            val consumerIn = Consumer(topdir, context)
            var count = 0
            for (ballot in consumerIn.iterateSpoiledBallots()) {
                println("$count ballot = ${ballot.ballotId}")
                assertTrue(ballot.ballotId.startsWith("ballot-id"))
                count++
            }
        }
    }

    @Test
    fun readTrustee() {
        runTest {
            val context = productionGroup()
            val initDir = "src/commonTest/data/runWorkflowAllAvailable"
            val consumerIn = Consumer(initDir, context)
            val init = consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }
            val trusteeDir = "src/commonTest/data/runWorkflowAllAvailable/private_data/trustees"
            init.guardians.forEach {
                val trustee = consumerIn.readTrustee(trusteeDir, it.guardianId)
                println("trustee = ${trustee}")
                assertTrue(trustee.id().equals(it.guardianId))
            }
        }
    }

    @Test
    fun readBadTrustee() {
        runTest {
            val context = productionGroup()
            val trusteeDir = "src/commonTest/data/runWorkflowAllAvailable/private_data/trustees"
            val consumerIn = Consumer(trusteeDir, context)
            val result: Result<DecryptingTrusteeIF, Throwable> = runCatching {
                consumerIn.readTrustee(trusteeDir, "badId")
            }
            assertTrue(result is Err)
            val message: String = result.getError()?.message ?: "not"
            assertTrue(message.contains("No such file"))
        }
    }

    @Test
    fun readMissingTrustees() {
        runTest {
            val context = productionGroup()
            val trusteeDir = "src/commonTest/data/testBad/nonexistant"
            val result: Result<DecryptingTrusteeIF, Throwable> = runCatching {
                val consumerIn = Consumer(trusteeDir, context)
                consumerIn.readTrustee(trusteeDir, "randomName")
            }
            assertFalse(result is Ok)
        }
    }

}