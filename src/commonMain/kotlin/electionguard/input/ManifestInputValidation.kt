package electionguard.input

import electionguard.ballot.Manifest
import electionguard.ballot.Manifest.VoteVariationType.approval
import electionguard.ballot.Manifest.VoteVariationType.one_of_m
import electionguard.ballot.Manifest.VoteVariationType.n_of_m
import mu.KotlinLogging

private val logger = KotlinLogging.logger("ManifestInputValidation")

/** Validate an election manifest, give human readable error information. */
class ManifestInputValidation(val manifest: Manifest) {
    private val gpUnits: Set<String> = manifest.geopoliticalUnits.map { it.geopoliticalUnitId }.toSet()
    private val gpUnitInStyles: Set<String> = manifest.ballotStyles.map { it.geopoliticalUnitIds }.flatten().toSet()
    private val candidates: Set<String> = manifest.candidates.map { it.candidateId }.toSet()
    private val parties: Set<String> = manifest.parties.map { it.partyId }.toSet()

    /** Determine if a manifest is valid.  */
    fun validate(): ValidationMessages {
        val manifestMessages = ValidationMessages("Manifest" + manifest.electionScopeId, 0)

        // Referential integrity of BallotStyle geopolitical_unit_ids
        for (ballotStyle in manifest.ballotStyles) {
            for (gpunit in ballotStyle.geopoliticalUnitIds) {
                if (!gpUnits.contains(gpunit)) {
                    val msg = "Manifest.A.1 BallotStyle '${ballotStyle.ballotStyleId}' has geopolitical_unit_id " +
                            "'$gpunit' that does not exist in manifest's geopolitical_units"
                    manifestMessages.add(msg)
                    logger.warn { msg }
                }
            }
        }

        // Referential integrity of Candidate party_id
        for (candidate in manifest.candidates) {
            if (candidate.partyId != null) {
                if (!parties.contains(candidate.partyId)) {
                    val msg = "Manifest.A.2 Candidate '${candidate.candidateId}' party_id '${candidate.partyId}' " +
                            "does not exist in manifest's Parties"
                    manifestMessages.add(msg)
                    logger.warn { msg }
                }
            }
        }

        val contestIds: MutableSet<String> = HashSet()
        val contestSeqs: MutableSet<Int> = HashSet()
        val selectionIds: MutableSet<String> = HashSet()
        for (electionContest in manifest.contests) {
            // No duplicate contest object_id
            if (contestIds.contains(electionContest.contestId)) {
                val msg = "Manifest.B.1 Multiple Contests have same id '${electionContest.contestId}'"
                manifestMessages.add(msg)
                logger.warn { msg }
            } else {
                contestIds.add(electionContest.contestId)
            }

            // No duplicate contest sequence
            if (contestSeqs.contains(electionContest.sequenceOrder)) {
                val msg = "Manifest.B.2 Multiple Contests have same sequence order ${electionContest.sequenceOrder}"
                manifestMessages.add(msg)
                logger.warn { msg }
            } else {
                contestSeqs.add(electionContest.sequenceOrder)
            }

            // No duplicate sequenceIds across contests
            for (electionSelection in electionContest.selections) {
                if (selectionIds.contains(electionSelection.selectionId)) {
                    val msg = "Manifest.B.6 Multiple Selections have same id '${electionSelection.selectionId}'" +
                            " within the manifest"
                    manifestMessages.add(msg)
                    logger.warn { msg }
                }
                selectionIds.add(electionSelection.selectionId)
            }
            validateContest(electionContest, manifestMessages)
        }
        return manifestMessages
    }

    /** Determine if the manifest contest is valid.  */
    fun validateContest(contest: Manifest.ContestDescription, ballotMesses: ValidationMessages) {
        val contestMesses = ballotMesses.nested("Contest" + contest.contestId)

        // Referential integrity of Contest electoral_district_id
        if (!gpUnits.contains(contest.geopoliticalUnitId)) {
            val msg = "Manifest.A.3 Contest's electoral_district_id '${contest.geopoliticalUnitId}'" +
                    " does not exist in manifest's GeopoliticalUnits"
            contestMesses.add(msg)
            logger.warn { msg }
        }

        // Contest electoral_district_id exists in some BallotStyle
        if (!gpUnitInStyles.contains(contest.geopoliticalUnitId)) {
            val msg = "Manifest.A.5 Contest's electoral_district_id '${contest.geopoliticalUnitId}'" +
                    " does not exist in any BallotStyle"
            contestMesses.add(msg)
            logger.warn { msg }
        }

        when (contest.voteVariation) {
            one_of_m, n_of_m, approval -> {}
            else -> {
                val msg = "Manifest.C.1 Contest's voteVariation '${contest.voteVariation}' not supported"
                contestMesses.add(msg)
                logger.warn { msg }
            }
        }

        if (contest.numberElected != contest.votesAllowed) {
            val msg = "Manifest.C.2 Contest's numberElected $contest.numberElected != $contest.votesAllowed votesAllowed"
            contestMesses.add(msg)
            logger.warn { msg }
        }

        when (contest.voteVariation) {
            one_of_m -> if (contest.votesAllowed != 1) {
                val msg = "Manifest.C.3 one_of_m Contest votesAllowed (${contest.votesAllowed}) must be 1"
                contestMesses.add(msg)
                logger.warn { msg }
            }
            n_of_m -> {
                if (contest.votesAllowed > contest.selections.size) {
                    val msg = "Manifest.C.4 n_of_m Contest votesAllowed (${contest.votesAllowed}) must be <= selections" +
                            " (${contest.selections.size})"
                    contestMesses.add(msg)
                    logger.warn { msg }
                }
            }
            approval -> {
                if (contest.votesAllowed != contest.selections.size) {
                    val msg = "Manifest.C.5 approval Contest votesAllowed (${contest.votesAllowed}) must equal " +
                            "number of selections (${contest.selections.size})"
                    contestMesses.add(msg)
                    logger.warn { msg }
                }
            }
            else -> {}
        }
        validateContestSelections(contest, contestMesses)
    }

    /** Determine if the manifest selection is valid.  */
    fun validateContestSelections(contest: Manifest.ContestDescription, contestMesses: ValidationMessages) {
        val selectionIds: MutableSet<String> = HashSet()
        val selectionSeqs: MutableSet<Int> = HashSet()
        val candidateIds: MutableSet<String> = HashSet()
        for (selection in contest.selections) {
            // No duplicate selection ids
            if (selectionIds.contains(selection.selectionId)) {
                val msg = "Manifest.B.3 Multiple Selections have same id '${selection.selectionId}'"
                contestMesses.add(msg)
                logger.warn { msg }
            } else {
                selectionIds.add(selection.selectionId)
            }

            // No duplicate selection sequence_order
            if (selectionSeqs.contains(selection.sequenceOrder)) {
                val msg = "Manifest.B.4 Multiple Selections have same sequence ${selection.sequenceOrder}"
                contestMesses.add(msg)
                logger.warn { msg }
            } else {
                selectionSeqs.add(selection.sequenceOrder)
            }

            // No duplicate selection candidates
            if (candidateIds.contains(selection.candidateId)) {
                val msg = "Manifest.B.5 Multiple Selections have same candidate id '${selection.candidateId}'"
                contestMesses.add(msg)
                logger.warn { msg }
            } else {
                candidateIds.add(selection.candidateId)
            }

            // Referential integrity of Selection candidate ids
            if (!candidates.contains(selection.candidateId)) {
                val msg = "Manifest.A.4 Ballot Selection '${selection.selectionId}' candidate_id '${selection.candidateId}'" +
                        " does not exist in manifest's Candidates"
                contestMesses.add(msg)
                logger.warn { msg }
            }
        }
    }

    // count the number of encryptions in a given ballot style
    // Map<BallotStyle: String, selectionCount: Int>
    fun countEncryptions(): Map<String, Int> {
        return manifest.styleToContestsMap.mapValues {
            it.value.map{ contest -> contest.countEncryptions() }.sum()
        }
    }

    // there will be one encryption for each selection and a placeholder for each voteAllowed
    private fun Manifest.ContestDescription.countEncryptions() = this.selections.size + this.votesAllowed

}