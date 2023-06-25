package electionguard.json2

import electionguard.ballot.ContestData
import electionguard.ballot.ContestDataStatus
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
    val decrypted_contest_data: DecryptedContestDataJson?, //  ballot decryption only
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
            contest.decryptedContestData?.publishJson(),
        )
    },
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
            contest.decrypted_contest_data?.import(group),
        ) },
)

@Serializable
data class DecryptedContestDataJson(
    val contest_data: ContestDataJson,
    val encrypted_contest_data: HashedElGamalCiphertextJson,  // matches EncryptedBallotContest.encrypted_contest_data
    val proof: ChaumPedersenJson,
    val beta: ElementModPJson, //  β = C0^s mod p ; needed to verify 10.2
)

fun DecryptedTallyOrBallot.DecryptedContestData.publishJson() = DecryptedContestDataJson(
    this.contestData.publishJson(), this.encryptedContestData.publishJson(),  this.proof.publishJson(), this.beta.publishJson(), )

fun DecryptedContestDataJson.import(group: GroupContext) = DecryptedTallyOrBallot.DecryptedContestData(
    this.contest_data.import(), this.encrypted_contest_data.import(group), this.proof.import(group), this.beta.import(group), )

// strawman for contest data (section 3.3.4)
// "any text written into one or more write-in text fields, information about overvotes, undervotes, and null
// votes, and possibly other data about voter selections"
@Serializable
data class ContestDataJson(
    val over_votes: List<Int>,  // list of selection sequence_number for this contest
    val write_ins: List<String>, //  list of write_in strings
    val status: String,
)

fun ContestData.publishJson() =
    ContestDataJson(this.overvotes, this.writeIns, this.status.name)

fun ContestDataJson.import() =
    ContestData(this.over_votes, this.write_ins, ContestDataStatus.valueOf(this.status))
