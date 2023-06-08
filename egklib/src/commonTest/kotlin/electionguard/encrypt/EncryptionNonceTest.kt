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
import electionguard.publish.makeConsumer
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Verify the embedded nonces in an Encrypted Ballot. */
class EncryptionNonceTest {
    val input = "src/commonTest/data/runWorkflowAllAvailable"
    val nballots = 11

    @Test
    fun testEncryptionNonces() {
        val group = productionGroup()
        val consumerIn = makeConsumer(input, group)
        val electionInit: ElectionInitialized =
            consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }

        // encrypt
        val encryptor = Encryptor(
            group,
            electionInit.manifest(),
            ElGamalPublicKey(electionInit.jointPublicKey),
            electionInit.extendedBaseHash
        )

        val starting = getSystemTimeInMillis()
        RandomBallotProvider(electionInit.manifest(), nballots).ballots().forEach { ballot ->
            val ciphertextBallot = encryptor.encrypt(ballot, null, 0)

            // decrypt with nonces
            val decryptionWithNonce = VerifyEmbeddedNonces(group, electionInit.manifest(), electionInit.jointPublicKey())
            val decryptedBallot = with (decryptionWithNonce) { ciphertextBallot.decrypt() }
            assertNotNull(decryptedBallot)

            compareBallots(ballot, decryptedBallot)
        }

        val took = getSystemTimeInMillis() - starting
        val msecsPerBallot = (took.toDouble() / nballots).roundToInt()
        println("testEncryptionNonces $nballots took $took millisecs for $nballots ballots = $msecsPerBallot msecs/ballot")
    }
}

fun compareBallots(ballot: PlaintextBallot, decryptedBallot: PlaintextBallot) {
    assertEquals(ballot.ballotId, decryptedBallot.ballotId)
    assertEquals(ballot.ballotStyleId, decryptedBallot.ballotStyleId)

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

// create our own class (instead of DecryptionWithEmbeddedNonces) in order to validate the embedded nonces
class VerifyEmbeddedNonces(val group : GroupContext, val manifest: Manifest, val publicKey: ElGamalPublicKey) {

    fun CiphertextBallot.decrypt(): PlaintextBallot {
        val ballotNonce: UInt256 = hashElements(UInt256.ONE, this.ballotId, this.ballotNonce) // TODO

        val plaintext_contests = mutableListOf<PlaintextBallot.Contest>()
        for (contest in this.contests) {
            val mcontest = manifest.contests.find { it.contestId == contest.contestId}
            assertNotNull(mcontest)
            val plaintextContest = verifyContestNonces(mcontest, ballotNonce, contest)
            assertNotNull(plaintextContest)
            plaintext_contests.add(plaintextContest)
        }
        return PlaintextBallot(
            this.ballotId,
            this.ballotStyleId,
            plaintext_contests,
            null
        )
    }

    private fun verifyContestNonces(
        mcontest: Manifest.ContestDescription,
        ballotNonce: UInt256,
        contest: CiphertextBallot.Contest
    ): PlaintextBallot.Contest {
        val contestDescriptionHash = mcontest.contestHash
        val contestDescriptionHashQ = contestDescriptionHash.toElementModQ(group)
        val nonceSequence = Nonces(contestDescriptionHashQ, ballotNonce)
        val contestNonce = nonceSequence[0]

        val plaintextSelections = mutableListOf<PlaintextBallot.Selection>()
        for (selection in contest.selections) {
            val mselection = mcontest.selections.find { it.selectionId == selection.selectionId }
            assertNotNull(mselection)
            val plaintextSelection = verifySelectionNonces(mselection, contestNonce, selection)
            assertNotNull(plaintextSelection)
            plaintextSelections.add(plaintextSelection)
        }
        return PlaintextBallot.Contest(
            contest.contestId,
            contest.sequenceOrder,
            plaintextSelections
        )
    }

    private fun verifySelectionNonces(
        mselection: Manifest.SelectionDescription,
        contestNonce: ElementModQ,
        selection: CiphertextBallot.Selection
    ): PlaintextBallot.Selection? {
        val nonceSequence = Nonces(mselection.selectionHash.toElementModQ(group), contestNonce) // TODO
        val selectionNonce: ElementModQ = nonceSequence[1]

        assertEquals(selectionNonce, selection.selectionNonce)

        val decodedVote: Int? = selection.ciphertext.decryptWithNonce(publicKey, selection.selectionNonce)
        return decodedVote?.let {
            PlaintextBallot.Selection(
                selection.selectionId,
                selection.sequenceOrder,
                decodedVote,
            )
        }
    }
}