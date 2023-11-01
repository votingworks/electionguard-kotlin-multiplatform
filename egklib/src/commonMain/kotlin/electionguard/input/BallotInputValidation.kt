package electionguard.input

import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.util.ErrorMessages
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger("BallotInputValidation")

/**
 * Determine if a ballot is valid and well-formed for the given manifest.
 * See [ElectionGuard Input Validation](https://github.com/danwallach/electionguard-kotlin-multiplatform/blob/main/docs/InputValidation.md)
 */
class BallotInputValidation(val manifest: Manifest) {
    private val contestMap = manifest.contests.associate { it.contestId  to ElectionContest(it) }
    private val styles = manifest.ballotStyles.associateBy { it.ballotStyleId }

    fun validate(ballot: PlaintextBallot): ErrorMessages {
        val ballotMesses = ErrorMessages("Ballot '${ballot.ballotId}'", 1)
        val ballotStyle: Manifest.BallotStyle? = styles[ballot.ballotStyle]
        
        // Referential integrity of ballot's BallotStyle id
        if (ballotStyle == null) {
            val msg = "Ballot.A.1 Ballot Style '${ballot.ballotStyle}' does not exist in election"
            ballotMesses.add(msg)
            logger.warn { msg }
        }

        val contestIds: MutableSet<String> = HashSet()
        for (ballotContest in ballot.contests) {
            // No duplicate contests
            if (contestIds.contains(ballotContest.contestId)) {
                val msg = "Ballot.B.1 Multiple Ballot contests have same contest id '${ballotContest.contestId}'"
                ballotMesses.add(msg)
                logger.warn { msg }
            } else {
                contestIds.add(ballotContest.contestId)
            }

            val contest: ElectionContest? = contestMap[ballotContest.contestId]
            // Referential integrity of ballotContest id
            if (contest == null) {
                val msg = "Ballot.A.2 Ballot Contest '${ballotContest.contestId}' does not exist in election"
                ballotMesses.add(msg)
                logger.warn { msg }
            } else if (ballotStyle != null) {
                validateContest(ballotContest, ballotStyle, contest, ballotMesses)
            }
        }
        return ballotMesses
    }

    /** Determine if contest is valid for the given election.  */
    private fun validateContest(
        ballotContest: PlaintextBallot.Contest,
        ballotStyle: Manifest.BallotStyle,
        electionContest: ElectionContest,
        ballotMesses: ErrorMessages
    ) {
        val contestMesses = ballotMesses.nested("Contest " + ballotContest.contestId)

        if (ballotContest.sequenceOrder != electionContest.sequenceOrder) {
            val msg = "Ballot.A.2.1 Ballot Contest '${ballotContest.contestId}' sequenceOrder ${ballotContest.sequenceOrder} " +
                    " does not match manifest contest sequenceOrder ${electionContest.sequenceOrder}"
            contestMesses.add(msg)
            logger.warn { msg }
        }

        // Contest geopoliticalUnitId ok for this BallotStyle
        if (!ballotStyle.geopoliticalUnitIds.contains(electionContest.geopoliticalUnitId)) {
            val msg = "Ballot.A.3 Contest's geopoliticalUnitId '${electionContest.geopoliticalUnitId}'" +
                    " not listed in BallotStyle '${ballotStyle.ballotStyleId}' geopoliticalUnitIds"
            contestMesses.add(msg)
            logger.warn { msg }
        }

        val selectionIds: MutableSet<String> = HashSet()
        val sequenceOrders: MutableSet<Int> = HashSet()
        for (selection in ballotContest.selections) {
            // No duplicate selections
            if (selectionIds.contains(selection.selectionId)) {
                val msg = "Ballot.B.2 Multiple Ballot selections have duplicate id '${selection.selectionId}'"
                contestMesses.add(msg)
                logger.warn { msg }
            } else {
                selectionIds.add(selection.selectionId)
            }
            if (sequenceOrders.contains(selection.sequenceOrder)) {
                val msg = "Ballot.B.3 Multiple Ballot selections have duplicate sequenceOrder '${selection.sequenceOrder}'"
                contestMesses.add(msg)
                logger.warn { msg }
            } else {
                sequenceOrders.add(selection.sequenceOrder)
            }
            val electionSelection: Manifest.SelectionDescription? = electionContest.selectionMap[selection.selectionId]
            // Referential integrity of ballotSelection id
            if (electionSelection == null) {
                val msg = "Ballot.A.4 Ballot Selection '${selection.selectionId}' does not exist in contest manifest"
                contestMesses.add(msg)
                logger.warn { msg }
            } else {
                if (selection.sequenceOrder != electionSelection.sequenceOrder) {
                    val msg = "Ballot.A.4.1 Ballot Selection '${selection.selectionId}' sequenceOrder ${selection.sequenceOrder} " +
                            " does not match manifest selection sequenceOrder ${electionSelection.sequenceOrder}"
                    contestMesses.add(msg)
                    logger.warn { msg }
                }

                // as of 10/2023
                // Voting limits are not enforced at ballot validation. They are marked as overvotes. So an overvote on one contest does not
                // invalidate the entire ballot.
            }
        }
    }

    class ElectionContest internal constructor(contest: Manifest.ContestDescription) {
        val contestId = contest.contestId
        val sequenceOrder = contest.sequenceOrder
        val geopoliticalUnitId = contest.geopoliticalUnitId
        val selectionMap = contest.selections.associateBy { it.selectionId }
        val contestLimit = contest.votesAllowed
        val optionLimit = contest.optionSelectionLimit
    }
}