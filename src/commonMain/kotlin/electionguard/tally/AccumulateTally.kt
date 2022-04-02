package electionguard.tally

import electionguard.ballot.CiphertextTally
import electionguard.ballot.Manifest
import electionguard.ballot.SubmittedBallot
import electionguard.core.ElGamalCiphertext
import electionguard.core.GroupContext
import electionguard.core.encryptedSum
import mu.KotlinLogging

private val logger = KotlinLogging.logger("AccumulateTally")

class AccumulateTally(val group : GroupContext, val manifest : Manifest, val name : String) {
    private val contests = manifest.contests.associate { it.contestId to Contest(it)}
    private val castIds = mutableSetOf<String>()

    fun addCastBallot(ballot: SubmittedBallot): Boolean {
        if (ballot.state != SubmittedBallot.BallotState.CAST) {
            return false
        }
        if (!this.castIds.add(ballot.ballotId)) {
            logger.warn { "Ballot ${ballot.ballotId} is duplicate"}
            return false
        }

        for (ballotContest in ballot.contests) {
            val contest = contests[ballotContest.contestId]
            if (contest == null) {
                logger.warn { "Ballot ${ballot.ballotId} has illegal contest ${ballotContest.contestId}"}
            } else {
                contest.accumulate(ballot.ballotId, ballotContest)
            }
        }
        return true
    }

    fun build(): CiphertextTally {
        val tallyContests = contests.values.associate { it.manifestContest.contestId to it.build() }
        return CiphertextTally(this.name, tallyContests)
    }

    private inner class Contest(val manifestContest : Manifest.ContestDescription) {
        private val selections = manifestContest.selections.associate { it.selectionId to Selection(it)}

        fun accumulate(ballotId : String, ballotContest: SubmittedBallot.Contest) {
            for (ballotSelection in ballotContest.selections) {
                if (ballotSelection.isPlaceholderSelection) {
                    continue
                }
                val selection = selections[ballotSelection.selectionId]
                if (selection == null) {
                    logger.warn { "Ballot $ballotId has illegal selection ${ballotSelection.selectionId} in contest ${ballotContest.contestId}"}
                } else {
                    selection.accumulate(ballotSelection.ciphertext)
                }
            }
        }

        fun build(): CiphertextTally.Contest {
            val tallySelections = selections.values.associate {it.manifestSelection.selectionId to it.build()}
            return CiphertextTally.Contest(
                manifestContest.contestId, manifestContest.sequenceOrder, manifestContest.cryptoHash, tallySelections)
        }
    }

    private inner class Selection(val manifestSelection : Manifest.SelectionDescription) {
        private val ciphertextAccumulate = ArrayList<ElGamalCiphertext>()

        fun accumulate(selection: ElGamalCiphertext) {
            ciphertextAccumulate.add(selection)
        }

        fun build(): CiphertextTally.Selection {
            return CiphertextTally.Selection(
                manifestSelection.selectionId, manifestSelection.sequenceOrder, manifestSelection.cryptoHash,
                ciphertextAccumulate.encryptedSum(),
            )
        }
    }
}