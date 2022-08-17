package electionguard.workflow

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.EncryptedBallot
import electionguard.core.ElGamalPublicKey
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.decrypt.DecryptionWithEmbeddedNonces
import electionguard.decrypt.DecryptionWithMasterNonce
import electionguard.encrypt.Encryptor
import electionguard.encrypt.submit
import electionguard.input.RandomBallotProvider
import electionguard.publish.Consumer
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** test workflow: encrypt ballot, decrypt with nonce, check match. */
class EncryptDecryptNonceTest {
    val input = "src/commonTest/data/runWorkflowAllAvailable"
    val nballots = 13

    @Test
    fun testDecryptionWithEmbeddedNonces() {
        val group = productionGroup()
        val consumerIn = Consumer(input, group)
        val electionInit: ElectionInitialized =
            consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }

        // encrypt
        val encryptor = Encryptor(
            group,
            electionInit.manifest(),
            ElGamalPublicKey(electionInit.jointPublicKey),
            electionInit.cryptoExtendedBaseHash
        )

        val starting = getSystemTimeInMillis()
        RandomBallotProvider(electionInit.manifest(), nballots).ballots().forEach { ballot ->
            val codeSeed = group.randomElementModQ(minimum = 2)
            val masterNonce = group.randomElementModQ(minimum = 2)
            val encryptedBallot = encryptor.encrypt(ballot, codeSeed, masterNonce, 0)

            // decrypt with nonces
            val decryptionWithNonces = DecryptionWithEmbeddedNonces(electionInit.jointPublicKey())
            val decryptedBallot = decryptionWithNonces.decrypt(encryptedBallot)
            assertNotNull(decryptedBallot)

            // all non zero votes match
            ballot.contests.forEach { contest1 ->
                val contest2 = decryptedBallot.contests.find { it.contestId == contest1.contestId }
                assertNotNull(contest2)
                contest1.selections.forEach { selection1 ->
                    val selection2 = contest2.selections.find { it.selectionId == selection1.selectionId }
                    assertNotNull(selection2)
                    assertEquals(selection1, selection2)
                }
            }

            // all votes match
            decryptedBallot.contests.forEach { contest2 ->
                val contest1 = decryptedBallot.contests.find { it.contestId == contest2.contestId }
                if (contest1 == null) {
                    contest2.selections.forEach { assertEquals(it.vote, 0) }
                } else {
                    contest2.selections.forEach { selection2 ->
                        val selection1 = contest1.selections.find { it.selectionId == selection2.selectionId }
                        if (selection1 == null) {
                            assertEquals(selection2.vote, 0)
                        } else {
                            assertEquals(selection1, selection2)
                        }
                    }
                }
            }
        }

        val took = getSystemTimeInMillis() - starting
        val msecsPerBallot = (took.toDouble() / nballots).roundToInt()
        println("testDecryptionWithEmbeddedNonces took $took millisecs for $nballots ballots = $msecsPerBallot msecs/ballot")
    }

    @Test
    fun testDecryptionWithMasterNonce() {
        val group = productionGroup()
        val consumerIn = Consumer(input, group)
        val electionInit: ElectionInitialized =
            consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }

        // encrypt
        val encryptor = Encryptor(
            group,
            electionInit.manifest(),
            ElGamalPublicKey(electionInit.jointPublicKey),
            electionInit.cryptoExtendedBaseHash
        )

        val starting = getSystemTimeInMillis()
        RandomBallotProvider(electionInit.manifest(), nballots).ballots().forEach { ballot ->
            val codeSeed = group.randomElementModQ(minimum = 2)
            val masterNonce = group.randomElementModQ(minimum = 2)
            val ciphertextBallot = encryptor.encrypt(ballot, codeSeed, masterNonce, 0)
            val encryptedBallot = ciphertextBallot.submit(EncryptedBallot.BallotState.CAST)

            // decrypt with nonces
            val decryptionWithMasterNonce = DecryptionWithMasterNonce(group, electionInit.jointPublicKey())
            val decryptedBallot = decryptionWithMasterNonce.decrypt(electionInit.manifest(), masterNonce, encryptedBallot)
            assertNotNull(decryptedBallot)

            // all non zero votes match
            ballot.contests.forEach { contest1 ->
                val contest2 = decryptedBallot.contests.find { it.contestId == contest1.contestId }
                assertNotNull(contest2)
                contest1.selections.forEach { selection1 ->
                    val selection2 = contest2.selections.find { it.selectionId == selection1.selectionId }
                    assertNotNull(selection2)
                    assertEquals(selection1, selection2)
                }
            }

            // all votes match
            decryptedBallot.contests.forEach { contest2 ->
                val contest1 = decryptedBallot.contests.find { it.contestId == contest2.contestId }
                if (contest1 == null) {
                    contest2.selections.forEach { assertEquals(it.vote, 0) }
                } else {
                    contest2.selections.forEach { selection2 ->
                        val selection1 = contest1.selections.find { it.selectionId == selection2.selectionId }
                        if (selection1 == null) {
                            assertEquals(selection2.vote, 0)
                        } else {
                            assertEquals(selection1, selection2)
                        }
                    }
                }
            }
        }

        val took = getSystemTimeInMillis() - starting
        val msecsPerBallot = (took.toDouble() / nballots).roundToInt()
        println("testDecryptionWithMasterNonce took $took millisecs for $nballots ballots = $msecsPerBallot msecs/ballot")
    }
}