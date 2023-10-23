package electionguard.tally

import electionguard.ballot.EncryptedBallotIF
import electionguard.ballot.EncryptedTally
import electionguard.ballot.ManifestIF
import electionguard.ballot.EncryptedBallot.BallotState
import electionguard.core.ElGamalCiphertext
import electionguard.core.GroupContext
import electionguard.core.encryptedSum
import mu.KotlinLogging

private val logger = KotlinLogging.logger("AccumulateTally")

/** Accumulate the votes of EncryptedBallots, and return an EncryptedTally. */
// TODO what happens if there are no EncryptedBallots?
class AccumulateTally(val group : GroupContext, val manifest : ManifestIF, val name : String) {
    private val mcontests = manifest.contests.associate { it.contestId to Contest(it)}
    private val castIds = mutableSetOf<String>()

    fun addCastBallot(ballot: EncryptedBallotIF): Boolean {
        if (ballot.state != BallotState.CAST) {
            logger.warn { "Ballot ${ballot.ballotId} does not have state CAST"}
            return false
        }
        if (!this.castIds.add(ballot.ballotId)) {
            logger.warn { "Ballot ${ballot.ballotId} is duplicate"}
            return false
        }

        for (ballotContest in ballot.contests) {
            val mcontest = mcontests[ballotContest.contestId]
            if (mcontest == null) {
                logger.warn { "Ballot ${ballot.ballotId} has contest ${ballotContest.contestId} not in manifest"}
            } else {
                mcontest.accumulate(ballot.ballotId, ballotContest)
            }
        }
        return true
    }

    fun build(): EncryptedTally {
        val tallyContests = mcontests.values.map { it.build() }
        return EncryptedTally(this.name, tallyContests, castIds.toList())
    }

    private inner class Contest(val manifestContest : ManifestIF.Contest) {
        private val selections = manifestContest.selections.associate { it.selectionId to Selection(it)}

        fun accumulate(ballotId : String, ballotContest: EncryptedBallotIF.Contest) {
            for (ballotSelection in ballotContest.selections) {
                val selection = selections[ballotSelection.selectionId]
                if (selection == null) {
                    logger.warn { "Ballot $ballotId has illegal selection ${ballotSelection.selectionId} in contest ${ballotContest.contestId}"}
                } else {
                    selection.accumulate(ballotSelection.encryptedVote)
                }
            }
        }

        fun build(): EncryptedTally.Contest {
            val tallySelections = selections.values.map { it.build() }
            return EncryptedTally.Contest(
                manifestContest.contestId, manifestContest.sequenceOrder, tallySelections)
        }
    }

    private inner class Selection(val manifestSelection : ManifestIF.Selection) {
        private val ciphertextAccumulate = ArrayList<ElGamalCiphertext>()

        fun accumulate(selection: ElGamalCiphertext) {
            ciphertextAccumulate.add(selection)
        }

        fun build(): EncryptedTally.Selection {
            return EncryptedTally.Selection(
                manifestSelection.selectionId, manifestSelection.sequenceOrder, ciphertextAccumulate.encryptedSum(),
            )
        }
    }
}