package electionguard.publish

import com.github.michaelbull.result.*
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.decrypt.DecryptingTrusteeIF
import org.junit.jupiter.api.Assertions.assertNotNull
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConsumerTest {
    private val group = productionGroup()
    val topdir = "src/commonTest/data/workflow/chainedJson"

    @Test
    fun consumerJson() {
        val consumerIn = makeConsumer(group, topdir)
        assertTrue(consumerIn.readElectionConfig() is Ok) // proto doesnt always have config, may have just init
        testConsumer(consumerIn)
    }

    @Test
    fun consumerProto() {
        testConsumer(makeConsumer(group, "src/commonTest/data/workflow/chainedProto"))
    }

    fun testConsumer(consumerIn : Consumer) {
        assertTrue( consumerIn.readElectionInitialized() is Ok)
        assertTrue( consumerIn.readTallyResult() is Ok)
        assertTrue( consumerIn.readDecryptionResult() is Ok)
        assertTrue( consumerIn.hasEncryptedBallots())
        assertTrue( consumerIn.encryptingDevices().isNotEmpty())
        val device = consumerIn.encryptingDevices()[0]

        assertTrue( consumerIn.readEncryptedBallotChain(device) is Ok)

        var iter = consumerIn.iterateEncryptedBallots(device) { true }
        assertNotNull(iter)
        assertTrue(iter.count() > 0)

        iter = consumerIn.iterateAllEncryptedBallots { true }
        assertNotNull(iter)
        assertTrue(iter.count() > 0)

        val iter2 = consumerIn.iterateDecryptedBallots()
        assertNotNull(iter2)
        assertTrue(iter.count() >= 0)
    }

    @Test
    fun readEncryptedBallots() {
        runTest {
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
            val consumerIn = makeConsumer(group, topdir)
            var count = 0
            for (ballot in consumerIn.iterateAllSpoiledBallots()) {
                println("$count ballot = ${ballot.ballotId}")
                count++
            }
        }
    }

    @Test
    fun readDecryptedBallots() {
        runTest {
            val consumerIn = makeConsumer(group, topdir)
            var count = 0
            for (tally in consumerIn.iterateDecryptedBallots()) {
                println("$count tally = ${tally.id}")
                count++
            }
        }
    }

    @Test
    fun readTrustee() {
        runTest {
            val consumerIn = makeConsumer(group, topdir)
            val result = consumerIn.readElectionInitialized()
            val init = result.unwrap()
            val trusteeDir = "$topdir/private_data/trustees"
            init.guardians.forEach {
                val trustee = consumerIn.readTrustee(trusteeDir, it.guardianId).unwrap()
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
                consumerIn.readTrustee(trusteeDir, "badId").unwrap()
            }
            assertTrue(result is Err)
            val message: String = result.getError()?.message ?: "not"
            assertTrue(message.contains("file does not exist"))
        }
    }

    @Test
    fun readMissingTrustees() {
        runTest {
            val trusteeDir = "src/commonTest/data/testBad/nonexistant"
            val group = productionGroup()
            val consumerIn = makeConsumer(group, topdir)
            val result: Result<DecryptingTrusteeIF, Throwable> = runCatching {
                consumerIn.readTrustee(trusteeDir, "randomName").unwrap()
            }
            assertFalse(result is Ok)
        }
    }

}