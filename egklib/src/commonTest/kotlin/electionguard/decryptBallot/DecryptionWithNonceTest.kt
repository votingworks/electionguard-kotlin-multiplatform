package electionguard.decryptBallot

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.makeContestData
import electionguard.core.*
import electionguard.encrypt.Encryptor
import electionguard.encrypt.submit
import electionguard.input.RandomBallotProvider
import electionguard.publish.makeConsumer
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class DecryptionWithNonceTest {
    val input = "src/commonTest/data/runWorkflowAllAvailable"
    private val nballots = 20

    /** test DecryptionWithPrimaryNonce: encrypt ballot, decrypt with master nonce, check match. */
    @Test
    fun testDecryptionWithPrimaryNonce() {
        val group = productionGroup()
        val consumerIn = makeConsumer(input, group)
        val init = consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }
        val encryptor = Encryptor(group, init.manifest(), ElGamalPublicKey(init.jointPublicKey), init.extendedBaseHash)

        RandomBallotProvider(init.manifest(), nballots).ballots().forEach { ballot ->
            val primaryNonce = UInt256.random()
            val ciphertextBallot = encryptor.encrypt(ballot, primaryNonce, 0)
            assertEquals(primaryNonce, ciphertextBallot.ballotNonce)
            val encryptedBallot = ciphertextBallot.submit(EncryptedBallot.BallotState.CAST)

            // decrypt with primary nonce
            val decryptionWithPrimaryNonce = DecryptionWithPrimaryNonce(group, init.manifest(), init.jointPublicKey(), init.extendedBaseHash)
            val decryptedBallotResult = with (decryptionWithPrimaryNonce) { encryptedBallot.decrypt(primaryNonce) }
            assertFalse(decryptedBallotResult is Err, "decryptionWithPrimaryNonce failed on ballot ${ballot.ballotId} errors = $decryptedBallotResult")
            val decryptedBallot = decryptedBallotResult.unwrap()

            // all non zero votes match
            ballot.contests.forEach { orgContest ->
                val contest2 = decryptedBallot.contests.find { it.contestId == orgContest.contestId }
                assertNotNull(contest2)
                orgContest.selections.forEach { selection1 ->
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
    }

    /** test DecryptionWithPrimaryNonce: encrypt ballot, decrypt with master nonce, check match. */
    @Test
    fun testDecryptionOfContestData() {
        val group = productionGroup()
        val consumerIn = makeConsumer(input, group)
        val init = consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }
        val encryptor = Encryptor(group, init.manifest(), ElGamalPublicKey(init.jointPublicKey), init.extendedBaseHash)

        val nb = 100
        RandomBallotProvider(init.manifest(), nb, true).ballots().forEach { ballot ->
            val primaryNonce = UInt256.random()
            val ciphertextBallot = encryptor.encrypt(ballot, primaryNonce, 0)
            val encryptedBallot = ciphertextBallot.submit(EncryptedBallot.BallotState.CAST)

            // decrypt with primary nonce
            val decryptionWithPrimaryNonce = DecryptionWithPrimaryNonce(group, init.manifest(), init.jointPublicKey(), init.extendedBaseHash)
            val decryptedBallotResult = with (decryptionWithPrimaryNonce) { encryptedBallot.decrypt(primaryNonce) }
            assertFalse(decryptedBallotResult is Err, "decryptionWithPrimaryNonce failed on ballot ${ballot.ballotId} errors = $decryptedBallotResult")
            val decryptedBallot = decryptedBallotResult.unwrap()

            // contestData matches
            ballot.contests.forEach { orgContest ->
                val mcontest = init.manifest().contests.find { it.contestId == orgContest.contestId }!!
                val orgContestData = makeContestData(mcontest.votesAllowed, orgContest.selections, orgContest.writeIns)

                val dcontest = decryptedBallot.contests.find { it.contestId == orgContest.contestId }!!
                assertEquals(dcontest.writeIns, orgContestData.writeIns)

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
        }
    }
}