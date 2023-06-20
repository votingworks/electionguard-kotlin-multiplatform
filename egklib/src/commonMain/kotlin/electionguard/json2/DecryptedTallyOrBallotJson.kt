package electionguard.json2

import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.core.GroupContext
import kotlinx.serialization.Serializable

@Serializable
data class DecryptedTallyOrBallotJson(
    val id: String,
    val contests: List<DecryptedContestJson>,
)

@Serializable
data class DecryptedContestJson(
    val contest_id: String,
    val selections: List<DecryptedSelectionJson>,
)

@Serializable
data class DecryptedSelectionJson(
    val selection_id: String,
    val tally: Int,
    val k_exp_tally: ElementModPJson, // eq 65
    val encrypted_vote: ElGamalCiphertextJson,
    val proof: ChaumPedersenJson,
)

fun DecryptedTallyOrBallot.publishJson() = DecryptedTallyOrBallotJson(
    this.id,
    this.contests.map { contest ->
        DecryptedContestJson(
            contest.contestId,
            contest.selections.map { selection ->
                DecryptedSelectionJson(
                    selection.selectionId,
                    selection.tally,
                    selection.kExpTally.publishJson(),
                    selection.encryptedVote.publishJson(),
                    selection.proof.publishJson(),
                ) },
        ) },
)

fun DecryptedTallyOrBallotJson.import(group: GroupContext) = DecryptedTallyOrBallot(
    this.id,
    this.contests.map { contest ->
        DecryptedTallyOrBallot.Contest(
            contest.contest_id,
            contest.selections.map { selection ->
                DecryptedTallyOrBallot.Selection(
                    selection.selection_id,
                    selection.tally,
                    selection.k_exp_tally.import(group),
                    selection.encrypted_vote.import(group),
                    selection.proof.import(group),
                ) },
        ) },
)
