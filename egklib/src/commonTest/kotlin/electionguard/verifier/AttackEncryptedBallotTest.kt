package electionguard.verifier

import com.github.michaelbull.result.Ok
import electionguard.ballot.ContestData
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.EncryptedTally
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.core.*
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.decrypt.DecryptorDoerre
import electionguard.decrypt.Guardians
import electionguard.decrypt.readDecryptingTrustees
import electionguard.publish.readElectionRecord
import electionguard.tally.AccumulateTally
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 An attack that modifies the EncryptedBallots before the tally phase.
 Could be a man-in-the-middle attack or an attack on the stored file.
 The strategy is just to switch two selection identifiers in some contest in the EncryptedBallot.
 The ids are used in the selection cryptoHash, so we need to modify the selection cryptoHash,
 which bubbles up to modifying the contest cryptoHash, and the ballot cryptoHash, and the tracking code.

 To detect, one needs to prove that the ballot cryptoHash hasnt been changed, eg the linear ballot chaining scheme,
 or a voter checking that their confirmation code matches on a spoiled ballot.
 But if the attacker is able to change the entire election record, they could modify the chained ballot codes.
 We are not currently using linear ballot chaining during encryption, so this library is currently susceptible to this attack.

 This attack bypasses contest limit and placeholder problems, and the proof remains valid.
 The attacker might switch votes in precincts where they know the likely vote ratio
*/

class AttackEncryptedBallotTest {
    private val inputDir   = "src/commonTest/data/workflow/allAvailableProto"
    private val trusteeDir = "$inputDir/private_data/trustees"
    private val showCount = true

    // @Test
    fun attackEncryptedBallots() {
        val group = productionGroup()
        val electionRecord = readElectionRecord(group, inputDir)

        val mungedBallots = mutableListOf<EncryptedBallot>()

        for (ballot in electionRecord.encryptedBallots { true }) {
            // println(" munged ballot ${ballot.ballotId}")
            mungedBallots.add(mungeBallot(ballot, ElGamalPublicKey(electionRecord.jointPublicKey()!!)))
        }

        if (showCount) {
            // sum it up
            val accumulator = AccumulateTally(group, electionRecord.manifest(), "attackedTally")
            for (encryptedBallot in mungedBallots ) {
                accumulator.addCastBallot(encryptedBallot)
            }
            val encryptedTally: EncryptedTally = accumulator.build()

            // decrypt it
            println("decrypt munged tally ")
            val mungedTally = decryptTally(group, encryptedTally, electionRecord.electionInit()!!,
                readDecryptingTrustees(group, inputDir, trusteeDir),
            )
            // println("tally for changed ballots = ${mungedTally.showTallies()}")

            val decryptedTally = electionRecord.decryptedTally()!!
            // println("original tally = ${result.decryptedTally.showTallies()}")

            compareTallies(decryptedTally, mungedTally, true)
            assertNotEquals(decryptedTally, mungedTally)
        }

        // verification still passes
        println("verify munged ballots ")
        val verifier = Verifier(electionRecord)
        val stats = Stats()
        val results = verifier.verifyEncryptedBallots(mungedBallots, stats)
        println("verify = ${results}")
        stats.show()
        assertTrue(results is Ok)
    }

    private fun mungeBallot(ballot: EncryptedBallot, publicKey: ElGamalPublicKey): EncryptedBallot {
        val ccontests = mutableListOf<EncryptedBallot.Contest>()
        for (contest in ballot.contests) {
            if (contest.contestId == "contest11") {
                ccontests.add(mungeContest(contest, publicKey))
            } else {
                ccontests.add(contest)
            }
        }

        //val contestHashes = ccontests.map { it.cryptoHash }
        //val cryptoHashMunged = hashElements(ballot.ballotId, ballot.manifestHash, contestHashes)
        //val trackingCodeMunged = hashElements(ballot.codeSeed, ballot.timestamp, cryptoHashMunged)

        return EncryptedBallot(
            ballot.ballotId,
            ballot.ballotStyleId,
            "device",
            ballot.timestamp,
            ByteArray(0),
            UInt256.random(),
            ccontests,
            ballot.state
        )
    }

    private fun mungeContest(contest: EncryptedBallot.Contest, publicKey: ElGamalPublicKey, ): EncryptedBallot.Contest {
        var idx55 = -1
        var idx56 = -1
        contest.selections.forEach {
            if (it.selectionId == "selection55") {
                idx55 = contest.selections.indexOf(it)
            }
            if (it.selectionId == "selection56") {
                idx56 = contest.selections.indexOf(it)
            }
        }

        // switch ben and john vote
        val sel44: EncryptedBallot.Selection = contest.selections[idx55]
        val sel45: EncryptedBallot.Selection = contest.selections[idx56]
        val switch44: EncryptedBallot.Selection = switchVote(sel44, sel45)
        val switch45: EncryptedBallot.Selection = switchVote(sel45, sel44)
        val selections2 = mutableListOf(*contest.selections.toTypedArray())
        selections2[idx55] = switch44
        selections2[idx56] = switch45

        // val changeCrypto = hashElements(contest.contestId, contest.cryptoHash, selections2)
        val contestData = ContestData(emptyList(), emptyList())
        // publicKey: ElGamalPublicKey, // aka K
        //        extendedBaseHash: UInt256, // aka He
        //        contestId: String, // aka Λ
        //        ballotNonce: UInt256,
        //        votesAllowed: Int
        return EncryptedBallot.Contest(
            contest.contestId,
            contest.sequenceOrder,
            contest.votesAllowed,
            contest.contestHash,
            selections2,
            contest.proof,
            contestData.encrypt(publicKey, UInt256.random(), "contestId", UInt256.random(), 1),
        )
    }

    // this fails in EncryptedBallot.Selection.is_valid_encryption() because the crypto_hash includes the
    // selection_id and the ciphertext.

    // switch the vote for the two selections TODO
    private fun switchVote(s1: EncryptedBallot.Selection, s2: EncryptedBallot.Selection): EncryptedBallot.Selection {
        return EncryptedBallot.Selection(
            s1.selectionId,
            s1.sequenceOrder,
            s2.encryptedVote,
            s2.proof,
        )
    }
}

fun decryptTally(
    group: GroupContext,
    encryptedTally: EncryptedTally,
    electionInit: ElectionInitialized,
    decryptingTrustees: List<DecryptingTrusteeIF>,
): DecryptedTallyOrBallot {
    val guardians = Guardians(group, electionInit.guardians)
    val decryptor = DecryptorDoerre(
        group,
        electionInit.extendedBaseHash,
        electionInit.jointPublicKey(),
        guardians,
        decryptingTrustees,
        )
    return with(decryptor) { encryptedTally.decrypt() }
}

fun compareTallies(
    tally1: DecryptedTallyOrBallot,
    tally2: DecryptedTallyOrBallot,
    diffOnly: Boolean,
) {
    println("Compare  ${tally1.id} to ${tally2.id}")
    val tally2ContestMap = tally2.contests.associateBy { it.contestId }
    tally1.contests.sortedBy { it.contestId }.forEach { contest1 ->
        if (!diffOnly) println(" Contest ${contest1.contestId}")
        val contest2 = tally2ContestMap[contest1.contestId] ?:
            throw IllegalStateException("Cant find contest ${contest1.contestId}")
        val tally2SelectionMap = contest2.selections.associateBy { it.selectionId }
        contest1.selections.sortedBy { it.selectionId }.forEach { selection1 ->
            val selection2 = tally2SelectionMap[selection1.selectionId] ?:
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
