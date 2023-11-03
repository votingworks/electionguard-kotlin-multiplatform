package electionguard.publish

import com.github.michaelbull.result.*
import electionguard.ballot.EncryptedBallot
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.util.ErrorMessages
import org.junit.jupiter.api.Assertions.assertNotNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConsumerTest {
    private val group = productionGroup()
    val topdir = "src/commonTest/data/workflow/chainedJson"

    @Test
    fun consumerJson() {
        val consumerIn = makeConsumer(group, topdir)
        assertTrue(consumerIn.isJson())
        assertTrue(consumerIn.readElectionConfig() is Ok) // proto doesnt always have config, may have just init
        testConsumer(consumerIn)
    }

    @Test
    fun consumerProto() {
        val consumerIn = makeConsumer(group, "src/commonTest/data/workflow/chainedProto")
        assertFalse(consumerIn.isJson())
        testConsumer(consumerIn)
    }

    fun testConsumer(consumerIn : Consumer) {
        assertTrue( consumerIn.readElectionInitialized() is Ok)
        assertTrue( consumerIn.readTallyResult() is Ok)
        assertTrue( consumerIn.readDecryptionResult() is Ok)
        assertTrue( consumerIn.hasEncryptedBallots())
        assertTrue( consumerIn.encryptingDevices().isNotEmpty())
        val device = consumerIn.encryptingDevices()[0]

        assertTrue( consumerIn.readEncryptedBallotChain(device) is Ok)

        val iterb = consumerIn.iterateEncryptedBallots(device) { true }
        assertNotNull(iterb)
        val iter = iterb.iterator()
        var count = 0
        var eballot : EncryptedBallot? = null
        while (iter.hasNext()) {
            eballot = iter.next()
            assertTrue(eballot is EncryptedBallot)
            count++
        }
        assertNotNull(eballot)
        assertEquals(count, iterb.count())

        val ballotDir = "${consumerIn.topdir()}/encrypted_ballots/$device"
        val readResult = consumerIn.readEncryptedBallot(ballotDir, eballot!!.ballotId)
        println(readResult)
        assertTrue (readResult is Ok)
        assertEquals(eballot, readResult.unwrap())

        val iterAll = consumerIn.iterateAllEncryptedBallots { true }
        assertNotNull(iter)
        assertTrue(iterAll.count() > 0)

        val iter2 = consumerIn.iterateDecryptedBallots()
        assertNotNull(iter2)
        assertTrue(iter2.count() > 0)

        val plaintextDir = "${consumerIn.topdir()}/private_data/input"
        val iter3 = consumerIn.iteratePlaintextBallots(plaintextDir) { true }
        assertNotNull(iter3)
        assertTrue(iter3.count() > 0)
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