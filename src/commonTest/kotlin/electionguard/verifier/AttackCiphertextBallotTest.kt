package electionguard.verifier

import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionInitialized
import electionguard.core.productionGroup
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.EncryptedTally
import electionguard.ballot.PlaintextTally
import electionguard.core.GroupContext
import electionguard.core.hashElements
import electionguard.decrypt.DecryptingMediator
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.decrypt.readDecryptingTrustees
import electionguard.publish.ElectionRecord
import electionguard.tally.AccumulateTally
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 An attack that modifies the EncryptedBallots before the tally phase.
 Could be a man-in-the-middle attack or an attack on the stored file.
 The strategy is just to switch two selection identifiers in some contest in the EncryptedBallot.
 The ids are used in the selection cryptoHash, so we need to modify the selection cryptoHash,
 which bubbles up to modifying the contest cryptoHash, and the ballot cryptoHash.

 To detect, one needs to prove that the ballot cryptoHash hasnt been changed, eg the linear ballot chaining scheme.
 But if they are able to change the ballot file, they could modify the chained ballot hashes.
 And we are not currently using linear ballot chaining during encryption, because its difficult with coroutines.
 So we are currently susceptible to this attack.

 This attack bypasses contest limit and placeholder problems, and the proof remains valid.
 The attacker might switch votes in precincts where they know the likely vote ratio
*/

class AttackEncryptedBallotTest {
    private val inputDir   = "src/commonTest/data/runWorkflowAllAvailable"
    private val trusteeDir = "src/commonTest/data/runWorkflowAllAvailable/private_data/trustees"
    private val showCount = true

    @Test
    fun attackEncryptedBallots() {
        val context = productionGroup()
        val electionRecordIn = ElectionRecord(inputDir, context)
        val mungedBallots = mutableListOf<EncryptedBallot>()

        for (ballot in electionRecordIn.iterateEncryptedBallots { true }) {
            // println(" munged ballot ${ballot.ballotId}")
            mungedBallots.add(mungeBallot(ballot))
        }

        if (showCount) {
            val electionInit = electionRecordIn.readElectionInitialized().unwrap()

            // sum it up
            val config = electionInit.config
            val accumulator = AccumulateTally(context, config.manifest, "attackedTally")
            for (encryptedBallot in mungedBallots ) {
                accumulator.addCastBallot(encryptedBallot)
            }
            val encryptedTally: EncryptedTally = accumulator.build()

            // decrypt it
            println("decrypt munged tally ")
            val mungedTally = decryptTally(context, encryptedTally, electionInit,
                readDecryptingTrustees(context, inputDir, trusteeDir),
            )
            // println("tally for changed ballots = ${mungedTally.showTallies()}")

            val result = electionRecordIn.readDecryptionResult().unwrap()
            // println("original tally = ${result.decryptedTally.showTallies()}")

            compareTallies(result.decryptedTally, mungedTally, true)
            assertNotEquals(result.decryptedTally, mungedTally)
        }

        // verification still passes
        println("verify munged ballots ")
        val verifier = Verifier(context, electionRecordIn, 11)
        val stats = verifier.verifyEncryptedBallots(mungedBallots)
        println("verify = ${stats.allOk}")
        if (!stats.allOk) println("  ${stats.errors}")
        assertTrue(stats.allOk)
    }

    private fun mungeBallot(ballot: EncryptedBallot): EncryptedBallot {
        val ccontests = mutableListOf<EncryptedBallot.Contest>()
        for (contest in ballot.contests) {
            if (contest.contestId == "contest11") {
                ccontests.add(mungeContest(contest))
            } else {
                ccontests.add(contest)
            }
        }

        val contestHashes = ccontests.map { it.cryptoHash }
        val cryptoHash = hashElements(ballot.ballotId, ballot.manifestHash, contestHashes)

        return EncryptedBallot(
            ballot.ballotId,
            ballot.ballotStyleId,
            ballot.manifestHash,
            ballot.codeSeed,
            ballot.code,
            ccontests,
            ballot.timestamp,
            cryptoHash,
            ballot.state
        )
    }

    private fun mungeContest(contest: EncryptedBallot.Contest): EncryptedBallot.Contest {
        var idx44 = -1
        var idx45 = -1
        contest.selections.forEach {
            if (it.selectionId == "selection44") {
                idx44 = contest.selections.indexOf(it)
            }
            if (it.selectionId == "selection45") {
                idx45 = contest.selections.indexOf(it)
            }
        }

        // switch ben and john vote
        val sel44: EncryptedBallot.Selection = contest.selections[idx44]
        val sel45: EncryptedBallot.Selection = contest.selections[idx45]
        val switch44: EncryptedBallot.Selection = switchVote(sel44, sel45)
        val switch45: EncryptedBallot.Selection = switchVote(sel45, sel44)
        val selections2 = mutableListOf(*contest.selections.toTypedArray())
        selections2[idx44] = switch44
        selections2[idx45] = switch45

        val changeCrypto = hashElements(contest.contestId, contest.cryptoHash, selections2)
        return EncryptedBallot.Contest(
            contest.contestId,
            contest.sequenceOrder,
            contest.contestHash,
            selections2,
            changeCrypto,
            contest.proof
        )
    }

    // this fails in EncryptedBallot.Selection.is_valid_encryption() because the crypto_hash includes the
    // selection_id and the ciphertext.

    // switch the vote for the two selections
    private fun switchVote(s1: EncryptedBallot.Selection, s2: EncryptedBallot.Selection): EncryptedBallot.Selection {
        val changeCryptoHash = hashElements(s1.selectionId, s1.selectionHash, s2.ciphertext.cryptoHashUInt256())
        //        val selectionId: String, // matches SelectionDescription.selectionId
        //        val sequenceOrder: Int, // matches SelectionDescription.sequenceOrder
        //        val selectionHash: UInt256, // matches SelectionDescription.cryptoHash
        //        val ciphertext: ElGamalCiphertext,
        //        val cryptoHash: UInt256,
        //        val isPlaceholderSelection: Boolean,
        //        val proof: DisjunctiveChaumPedersenProofKnownNonce,
        //        val extendedData: HashedElGamalCiphertext?,
        return EncryptedBallot.Selection(
            s1.selectionId, s1.sequenceOrder, s1.selectionHash,
            s2.ciphertext,
            changeCryptoHash,
            s2.isPlaceholderSelection, s2.proof, s2.extendedData,
        )
    }
}

fun decryptTally(
    group: GroupContext,
    encryptedTally: EncryptedTally,
    electionInit: ElectionInitialized,
    decryptingTrustees: List<DecryptingTrusteeIF>,
): PlaintextTally {
    val decryptor = DecryptingMediator(group, electionInit, decryptingTrustees, emptyList())
    return with(decryptor) { encryptedTally.decrypt() }
}

fun compareTallies(
    tally1: PlaintextTally,
    tally2: PlaintextTally,
    diffOnly: Boolean,
) {
    println("Compare  ${tally1.tallyId} to ${tally2.tallyId}")
    tally1.contests.values.sortedBy { it.contestId }.forEach { contest1 ->
        if (!diffOnly) println(" Contest ${contest1.contestId}")
        val contest2 = tally2.contests[contest1.contestId] ?:
            throw IllegalStateException("Cant find contest ${contest1.contestId}")
        contest1.selections.values.sortedBy { it.selectionId }.forEach { selection1 ->
            val selection2 = contest2.selections[selection1.selectionId] ?:
                throw IllegalStateException("Cant find selection ${selection1.selectionId}")
            val same = selection1.tally == selection2.tally
            if (!diffOnly) {
                println("  Selection ${selection1.selectionId}: ${selection1.tally} vs ${selection2.tally}" +
                        if (same) "" else "*********")
            } else if (!same ){
                println("  Selection ${contest1.contestId}/${selection1.selectionId}: ${selection1.tally} != ${selection2.tally}")
            }
        }
    }
}
