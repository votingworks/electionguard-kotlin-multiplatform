package electionguard.publish

import com.github.michaelbull.result.*
import electionguard.ballot.protocolVersion
import electionguard.cli.ManifestBuilder.Companion.electionScopeId
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.decrypt.DecryptingTrusteeIF
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConsumerTest {
    private val topdir = "src/commonTest/data/workflow/someAvailableJson"

    @Test
    fun readElectionRecord() {
        runTest {
            val group = productionGroup()
            val electionRecord = readElectionRecord(group, topdir)
            val manifest = electionRecord.manifest()
            println("electionRecord.manifest.specVersion = ${electionRecord.manifest().specVersion}")
            assertEquals(electionScopeId, manifest.electionScopeId)
            assertEquals(protocolVersion, manifest.specVersion)
        }
    }

    @Test
    fun readSpoiledBallotTallys() {
        runTest {
            val group = productionGroup()
            val consumerIn = makeConsumer(group, topdir)
            var count = 0
            for (tally in consumerIn.iterateDecryptedBallots()) {
                println("$count tally = ${tally.id}")
                count++
            }
        }
    }

    @Test
    fun readEncryptedBallots() {
        runTest {
            val group = productionGroup()
            val consumerIn = makeConsumer(group, topdir)
            var count = 0
            for (ballot in consumerIn.iterateAllEncryptedBallots { true} ) {
                println("$count ballot = ${ballot.ballotId}")
                count++
            }
        }
    }

    @Test
    fun readEncryptedBallotsCast() {
        runTest {
            val group = productionGroup()
            val consumerIn = makeConsumer(group, topdir)
            var count = 0
            for (ballot in consumerIn.iterateAllCastBallots()) {
                println("$count ballot = ${ballot.ballotId}")
                count++
            }
        }
    }

    @Test
    fun readSubmittedBallotsSpoiled() {
        runTest {
            val group = productionGroup()
            val consumerIn = makeConsumer(group, topdir)
            var count = 0
            for (ballot in consumerIn.iterateAllSpoiledBallots()) {
                println("$count ballot = ${ballot.ballotId}")
                count++
            }
        }
    }

    @Test
    fun readTrustee() {
        runTest {
            val group = productionGroup()
            val consumerIn = makeConsumer(group, topdir)
            val init = consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }
            val trusteeDir = "$topdir/private_data/trustees"
            init.guardians.forEach {
                val trustee = consumerIn.readTrustee(trusteeDir, it.guardianId)
                assertTrue(trustee.id().equals(it.guardianId))
            }
        }
    }

    @Test
    fun readBadTrustee() {
        runTest {
            val trusteeDir = "$topdir/private_data/trustees"
            val group = productionGroup()
            val consumerIn = makeConsumer(group, topdir)
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
            val trusteeDir = "src/commonTest/data/testBad/nonexistant"
            val group = productionGroup()
            val consumerIn = makeConsumer(group, topdir)
            val result: Result<DecryptingTrusteeIF, Throwable> = runCatching {
                consumerIn.readTrustee(trusteeDir, "randomName")
            }
            assertFalse(result is Ok)
        }
    }

}