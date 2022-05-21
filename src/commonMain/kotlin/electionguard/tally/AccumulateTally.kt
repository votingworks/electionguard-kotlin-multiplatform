package electionguard.tally

import electionguard.ballot.EncryptedTally
import electionguard.ballot.Manifest
import electionguard.ballot.EncryptedBallot
import electionguard.core.ElGamalCiphertext
import electionguard.core.GroupContext
import electionguard.core.encryptedSum
import mu.KotlinLogging

private val logger = KotlinLogging.logger("AccumulateTally")

class AccumulateTally(val group : GroupContext, val manifest : Manifest, val name : String) {
    private val contests = manifest.contests.associate { it.contestId to Contest(it)}
    private val castIds = mutableSetOf<String>()

    fun addCastBallot(ballot: EncryptedBallot): Boolean {
        if (ballot.state != EncryptedBallot.BallotState.CAST) {
            return false
        }
        if (!this.castIds.add(ballot.ballotId)) {
            logger.warn { "Ballot ${ballot.ballotId} is duplicate"}
            return false
        }

        for (ballotContest in ballot.contests) {
            val contest = contests[ballotContest.contestId]
            if (contest == null) {
                logger.warn { "Ballot ${ballot.ballotId} has contest ${ballotContest.contestId} not in manifest"}
            } else {
                contest.accumulate(ballot.ballotId, ballotContest)
            }
        }
        return true
    }

    fun build(): EncryptedTally {
        val tallyContests = contests.values.map { it.build() }
        return EncryptedTally(this.name, tallyContests)
    }

    fun ballotIds(): List<String> {
        return castIds.toList()
    }

    private inner class Contest(val manifestContest : Manifest.ContestDescription) {
        private val selections = manifestContest.selections.associate { it.selectionId to Selection(it)}

        fun accumulate(ballotId : String, ballotContest: EncryptedBallot.Contest) {
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

        fun build(): EncryptedTally.Contest {
            val tallySelections = selections.values.map { it.build() }
            return EncryptedTally.Contest(
                manifestContest.contestId, manifestContest.sequenceOrder, manifestContest.cryptoHash, tallySelections)
        }
    }

    private inner class Selection(val manifestSelection : Manifest.SelectionDescription) {
        private val ciphertextAccumulate = ArrayList<ElGamalCiphertext>()

        fun accumulate(selection: ElGamalCiphertext) {
            ciphertextAccumulate.add(selection)
        }

        fun build(): EncryptedTally.Selection {
            return EncryptedTally.Selection(
                manifestSelection.selectionId, manifestSelection.sequenceOrder, manifestSelection.cryptoHash,
                ciphertextAccumulate.encryptedSum(),
            )
        }
    }
}