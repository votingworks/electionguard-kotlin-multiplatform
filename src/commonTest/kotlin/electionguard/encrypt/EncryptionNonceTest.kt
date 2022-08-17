package electionguard.encrypt

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.Nonces
import electionguard.core.UInt256
import electionguard.core.decryptWithNonce
import electionguard.core.get
import electionguard.core.getSystemTimeInMillis
import electionguard.core.hashElements
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.core.toElementModQ
import electionguard.input.RandomBallotProvider
import electionguard.publish.Consumer
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Test the embedded nonces in an Encrypted Ballot. */
class EncryptionNonceTest {
    val input = "src/commonTest/data/runWorkflowAllAvailable"
    val nballots = 11

    @Test
    fun testEncryptionWithMasterNonce() {
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
            val decryptionWithNonce = DecryptionTestNonce(group, electionInit.jointPublicKey())
            val decryptedBallot = decryptionWithNonce.decrypt(electionInit.manifest(), masterNonce, encryptedBallot)
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
        println("testEncryptionWithMasterNonce $nballots took $took millisecs for $nballots ballots = $msecsPerBallot msecs/ballot")
    }
}

class DecryptionTestNonce(val group : GroupContext, val publicKey: ElGamalPublicKey) {

    fun decrypt(manifest: Manifest, masterNonce: ElementModQ, ballot: CiphertextBallot): PlaintextBallot {
        val ballotNonce: UInt256 = hashElements(manifest.cryptoHash, ballot.ballotId, masterNonce)

        val plaintext_contests = mutableListOf<PlaintextBallot.Contest>()
        for (contest in ballot.contests) {
            val mcontest = manifest.contests.find { it.contestId == contest.contestId}
            assertNotNull(mcontest)
            val plaintextContest = decryptContestWithNonce(mcontest, ballotNonce, contest)
            assertNotNull(plaintextContest)
            plaintext_contests.add(plaintextContest)
        }
        return PlaintextBallot(
            ballot.ballotId,
            ballot.ballotStyleId,
            plaintext_contests,
            null
        )
    }

    private fun decryptContestWithNonce(
        mcontest: Manifest.ContestDescription,
        ballotNonce: UInt256,
        contest: CiphertextBallot.Contest
    ): PlaintextBallot.Contest {
        val contestDescriptionHash = mcontest.cryptoHash
        val contestDescriptionHashQ = contestDescriptionHash.toElementModQ(group)
        val nonceSequence = Nonces(contestDescriptionHashQ, ballotNonce)
        val contestNonce = nonceSequence[contest.sequenceOrder]
        val chaumPedersenNonce = nonceSequence[0]

        assertEquals(contestNonce, contest.contestNonce)

        val plaintextSelections = mutableListOf<PlaintextBallot.Selection>()
        for (selection in contest.selections.filter { !it.isPlaceholderSelection }) {
            val mselection = mcontest.selections.find { it.selectionId == selection.selectionId }
            assertNotNull(mselection)
            val plaintextSelection = decryptSelectionWithNonce(mselection, contestNonce, selection)
            assertNotNull(plaintextSelection)
            plaintextSelections.add(plaintextSelection)
        }
        return PlaintextBallot.Contest(
            contest.contestId,
            contest.sequenceOrder,
            plaintextSelections
        )
    }

    private fun decryptSelectionWithNonce(
        mselection: Manifest.SelectionDescription,
        contestNonce: ElementModQ,
        selection: CiphertextBallot.Selection
    ): PlaintextBallot.Selection? {
        val nonceSequence = Nonces(mselection.cryptoHash.toElementModQ(group), contestNonce)
        val disjunctiveChaumPedersenNonce: ElementModQ = nonceSequence[0]
        val selectionNonce: ElementModQ = nonceSequence[selection.sequenceOrder]

        assertEquals(selectionNonce, selection.selectionNonce)

        val decodedVote: Int? = selection.ciphertext.decryptWithNonce(publicKey, selection.selectionNonce)
        return decodedVote?.let {
            PlaintextBallot.Selection(
                selection.selectionId,
                selection.sequenceOrder,
                decodedVote,
                null
            )
        }
    }
}