package electionguard.input

import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import mu.KotlinLogging

private val logger = KotlinLogging.logger("BallotInputValidation")

class BallotInputValidation(val election: Manifest) {

    val contestMap = election.contests.associate { it.contestId to ElectionContest(it) }
    val styles = election.ballotStyles.associateBy { it.ballotStyleId }

    /** Determine if a ballot is valid and well-formed for the given election. */
    fun validate(ballot: PlaintextBallot): ValidationMessages {
        val ballotMesses = ValidationMessages("Ballot" + ballot.ballotId, 0)
        val ballotStyle: Manifest.BallotStyle? = styles.get(ballot.ballotStyleId)

        // Referential integrity of ballot's BallotStyle id
        if (ballotStyle == null) {
            val msg = "Ballot.A.1 Ballot Style '${ballot.ballotStyleId}' does not exist in election"
            ballotMesses.add(msg)
            logger.warn { msg }
        }

        val contestIds: MutableSet<String> = HashSet()
        for (ballotContest in ballot.contests) {
            // No duplicate contests
            if (contestIds.contains(ballotContest.contestId)) {
                val msg =
                    "Ballot.B.1 Multiple Ballot contests have same id '${ballotContest.contestId}'"
                ballotMesses.add(msg)
                logger.warn { msg }
            } else {
                contestIds.add(ballotContest.contestId)
            }

            val contest: ElectionContest? = contestMap.get(ballotContest.contestId)
            // Referential integrity of ballotContest id
            if (contest == null) {
                val msg =
                    "Ballot.A.2 Ballot Contest '${ballotContest.contestId}' does not exist in " +
                        "election"
                ballotMesses.add(msg)
                logger.warn { msg }
            } else if (ballotStyle != null) {
                validateContest(ballotContest, ballotStyle, contest, ballotMesses)
            }
        }
        return ballotMesses
    }

    /** Determine if contest is valid for the given election. */
    fun validateContest(
        ballotContest: PlaintextBallot.Contest,
        ballotStyle: Manifest.BallotStyle,
        electionContest: ElectionContest,
        ballotMesses: ValidationMessages
    ) {
        val contestMesses = ballotMesses.nested("Contest " + ballotContest.contestId)

        // Contest geopoliticalUnitId ok for this BallotStyle
        if (!ballotStyle.geopoliticalUnitIds.contains(electionContest.geopoliticalUnitId)) {
            val msg =
                "Ballot.A.3 Contest's geopoliticalUnitId '${electionContest.geopoliticalUnitId}'" +
                    " not listed in BallotStyle '${ballotStyle.ballotStyleId}' geopoliticalUnitIds"
            contestMesses.add(msg)
            logger.warn { msg }
        }

        var total = 0
        val selectionIds: MutableSet<String> = HashSet()
        val sequenceOrders: MutableSet<Int> = HashSet()
        for (selection in ballotContest.selections) {
            // No duplicate selections
            if (selectionIds.contains(selection.selectionId)) {
                val msg =
                    "Ballot.B.2 Multiple Ballot selections have duplicate id '" +
                        "${selection.selectionId}'"
                contestMesses.add(msg)
                logger.warn { msg }
            } else {
                selectionIds.add(selection.selectionId)
            }
            if (sequenceOrders.contains(selection.sequenceOrder)) {
                val msg =
                    "Ballot.B.3 Multiple Ballot selections have duplicate sequenceOrder '" +
                        "${selection.sequenceOrder}'"
                contestMesses.add(msg)
                logger.warn { msg }
            } else {
                sequenceOrders.add(selection.sequenceOrder)
            }
            val electionSelection: Manifest.SelectionDescription? =
                electionContest.selectionMap[selection.selectionId]
            // Referential integrity of ballotSelection id
            if (electionSelection == null) {
                val msg =
                    "Ballot.B.2.1 Ballot Selection '${selection.selectionId}' does not exist in " +
                        "contest manifest"
                contestMesses.add(msg)
                logger.warn { msg }
            } else {
                // Vote can only be a 0 or 1
                if (selection.vote < 0 || selection.vote > 1) {
                    val msg =
                        "Ballot.C.1 Ballot Selection '${selection.selectionId}' vote ($selection" +
                            ".vote) must be 0 or 1"
                    contestMesses.add(msg)
                    logger.warn { msg }
                } else {
                    total += selection.vote
                }
            }
        }

        // Total votes for contest exceeds allowed limit
        if (total > electionContest.allowed) {
            val msg =
                "Ballot.C.2 Ballot Selection votes ($total) exceeds limit (" +
                    "${electionContest.allowed})"
            contestMesses.add(msg)
            logger.warn { msg }
        }
    }

    class ElectionContest internal constructor(electionContest: Manifest.ContestDescription) {
        val contestId = electionContest.contestId
        val geopoliticalUnitId = electionContest.geopoliticalUnitId
        val allowed = electionContest.votesAllowed
        val selectionMap = electionContest.selections.associateBy { it.selectionId }
    }
}