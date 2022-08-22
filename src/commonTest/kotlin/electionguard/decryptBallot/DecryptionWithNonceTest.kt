package electionguard.decryptBallot

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.EncryptedBallot
import electionguard.core.ElGamalPublicKey
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.encrypt.Encryptor
import electionguard.encrypt.submit
import electionguard.input.RandomBallotProvider
import electionguard.publish.Consumer
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class DecryptionWithNonceTest {
    val input = "src/commonTest/data/runWorkflowAllAvailable"
    val nballots = 20

    /** test DecryptionWithEmbeddedNonces: encrypt ballot, decrypt with embedded nonces, check match. */
    @Test
    fun testDecryptionWithEmbeddedNonces() {
        val group = productionGroup()
        val consumerIn = Consumer(input, group)
        val electionInit: ElectionInitialized =
            consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }
        val encryptor = Encryptor(
            group,
            electionInit.manifest(),
            ElGamalPublicKey(electionInit.jointPublicKey),
            electionInit.cryptoExtendedBaseHash
        )

        var encryptTime = 0L
        var decryptTime = 0L
        RandomBallotProvider(electionInit.manifest(), nballots).ballots().forEach { ballot ->
            val startEncrypt = getSystemTimeInMillis()
            val codeSeed = group.randomElementModQ(minimum = 2)
            val masterNonce = group.randomElementModQ(minimum = 2)
            val encryptedBallot = encryptor.encrypt(ballot, codeSeed, masterNonce, 0)
            encryptTime += getSystemTimeInMillis() - startEncrypt

            // decrypt with nonces and check
            val startDecrypt = getSystemTimeInMillis()
            val decryptionWithNonces = DecryptionWithEmbeddedNonces(electionInit.jointPublicKey())
            val decryptedBallotResult = with (decryptionWithNonces) { encryptedBallot.decrypt() }
            assertFalse(decryptedBallotResult is Err, "decryptionWithNonces failed on ballot ${ballot.ballotId} errors = $decryptedBallotResult")
            val decryptedBallot = decryptedBallotResult.unwrap()

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
            decryptTime += getSystemTimeInMillis() - startDecrypt
        }

        val encryptPerBallot = (encryptTime.toDouble() / nballots).roundToInt()
        val decryptPerBallot = (decryptTime.toDouble() / nballots).roundToInt()
        println("testDecryptionWithEmbeddedNonces for $nballots ballots took $encryptPerBallot encrypt, $decryptPerBallot decrypt msecs/ballot")
    }

    /** test DecryptionWithMasterNonce: encrypt ballot, decrypt with master nonce, check match. */
    @Test
    fun testDecryptionWithMasterNonce() {
        val group = productionGroup()
        val consumerIn = Consumer(input, group)
        val electionInit: ElectionInitialized =
            consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }
        val encryptor = Encryptor(
            group,
            electionInit.manifest(),
            ElGamalPublicKey(electionInit.jointPublicKey),
            electionInit.cryptoExtendedBaseHash
        )

        var encryptTime = 0L
        var decryptTime = 0L
        RandomBallotProvider(electionInit.manifest(), nballots).ballots().forEach { ballot ->
            val startEncrypt = getSystemTimeInMillis()
            val codeSeed = group.randomElementModQ(minimum = 2)
            val masterNonce = group.randomElementModQ(minimum = 2)
            val ciphertextBallot = encryptor.encrypt(ballot, codeSeed, masterNonce, 0)
            val encryptedBallot = ciphertextBallot.submit(EncryptedBallot.BallotState.CAST)
            encryptTime += getSystemTimeInMillis() - startEncrypt

            // decrypt with master nonce and check
            val startDecrypt = getSystemTimeInMillis()
            val decryptionWithMasterNonce = DecryptionWithMasterNonce(group, electionInit.manifest(), electionInit.jointPublicKey())
            val decryptedBallotResult = with (decryptionWithMasterNonce) { encryptedBallot.decrypt(masterNonce) }
            assertFalse(decryptedBallotResult is Err, "decryptionWithMasterNonce failed on ballot ${ballot.ballotId} errors = $decryptedBallotResult")
            val decryptedBallot = decryptedBallotResult.unwrap()

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
            decryptTime += getSystemTimeInMillis() - startDecrypt
        }

        val encryptPerBallot = (encryptTime.toDouble() / nballots).roundToInt()
        val decryptPerBallot = (decryptTime.toDouble() / nballots).roundToInt()
        println("testDecryptionWithMasterNonce for $nballots ballots took $encryptPerBallot encrypt, $decryptPerBallot decrypt msecs/ballot")
    }
}