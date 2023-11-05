package electionguard.tally

import electionguard.ballot.EncryptedBallotIF
import electionguard.ballot.EncryptedTally
import electionguard.ballot.ManifestIF
import electionguard.ballot.EncryptedBallot.BallotState
import electionguard.core.*
import electionguard.util.ErrorMessages
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger("AccumulateTally")

/** Accumulate the votes of EncryptedBallots, and return a new EncryptedTally. */
class AccumulateTally(
    val group: GroupContext,
    val manifest: ManifestIF,
    val name: String,
    val extendedBaseHash: UInt256,
    val jointPublicKey: ElGamalPublicKey,
) {
    private val contests = manifest.contests.associate { it.contestId to Contest(it) }
    private val castIds = mutableSetOf<String>()

    fun addCastBallot(ballot: EncryptedBallotIF, errs: ErrorMessages): Boolean {
        if (ballot.state != BallotState.CAST) {
            errs.add("Ballot ${ballot.ballotId} does not have state CAST")
            return false
        }
        if (ballot.electionId != extendedBaseHash) {
            errs.add("Ballot ${ballot.ballotId} has wrong electionId ${ballot.electionId}")
            return false
        }
        if (!this.castIds.add(ballot.ballotId)) {
            errs.add("Ballot ${ballot.ballotId} is duplicate")
            return false
        }

        for (ballotContest in ballot.contests) {
            val contest = contests[ballotContest.contestId]
            if (contest == null) {
                errs.add("Ballot ${ballot.ballotId} has contest ${ballotContest.contestId} not in manifest")
            } else {
                contest.accumulate(ballot.ballotId, ballotContest, errs.nested("Contest ${ballotContest.contestId}"))
            }
        }
        return true
    }

    fun build(): EncryptedTally {
        val tallyContests = contests.values.map { it.build() }
        return EncryptedTally(this.name, tallyContests, castIds.toList(), extendedBaseHash)
    }

    private inner class Contest(val manifestContest: ManifestIF.Contest) {
        private val selections = manifestContest.selections.associate { it.selectionId to Selection(it) }

        fun accumulate(ballotId: String, ballotContest: EncryptedBallotIF.Contest, errs: ErrorMessages) {
            for (ballotSelection in ballotContest.selections) {
                val selection = selections[ballotSelection.selectionId]
                if (selection == null) {
                    errs.add("Ballot $ballotId has illegal selection ${ballotSelection.selectionId} in contest ${ballotContest.contestId}")
                } else {
                    selection.accumulate(ballotSelection.encryptedVote)
                }
            }
        }

        fun build(): EncryptedTally.Contest {
            val tallySelections = selections.values.map { it.build() }
            return EncryptedTally.Contest(
                manifestContest.contestId, manifestContest.sequenceOrder, tallySelections
            )
        }
    }

    private inner class Selection(val manifestSelection: ManifestIF.Selection) {
        private val ciphertextAccumulate = mutableListOf<ElGamalCiphertext>()

        fun accumulate(selection: ElGamalCiphertext) {
            ciphertextAccumulate.add(selection)
        }

        fun build(): EncryptedTally.Selection {
            return EncryptedTally.Selection(
                manifestSelection.selectionId,
                manifestSelection.sequenceOrder,
                ciphertextAccumulate.encryptedSum() ?: 0.encrypt(jointPublicKey),
            )
        }
    }
}