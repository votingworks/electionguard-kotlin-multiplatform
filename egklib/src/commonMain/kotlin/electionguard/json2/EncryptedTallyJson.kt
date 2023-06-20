package electionguard.json2

import electionguard.ballot.EncryptedTally
import electionguard.core.GroupContext
import kotlinx.serialization.Serializable

@Serializable
data class EncryptedTallyJson(
    val tally_id: String,
    val contests: List<EncryptedTallyContestJson>,
)

@Serializable
data class EncryptedTallyContestJson(
    val contest_id: String,
    val sequence_order: Int,
    val selections: List<EncryptedTallySelectionJson>,
)

@Serializable
data class EncryptedTallySelectionJson(
    val selection_id: String,
    val sequence_order: Int,
    val encrypted_vote: ElGamalCiphertextJson,
)

fun EncryptedTally.publishJson(): EncryptedTallyJson {
    val contests = this.contests.map { pcontest ->

        EncryptedTallyContestJson(
            pcontest.contestId,
            pcontest.sequenceOrder,
            pcontest.selections.map {
                EncryptedTallySelectionJson(
                    it.selectionId,
                    it.sequenceOrder,
                    it.encryptedVote.publishJson(),
                )
            })
    }
    return EncryptedTallyJson(this.tallyId, contests)
}

fun EncryptedTallyJson.import(group: GroupContext): EncryptedTally {
    val contests = this.contests.map { pcontest ->

        EncryptedTally.Contest(
            pcontest.contest_id,
            pcontest.sequence_order,
            pcontest.selections.map {
                EncryptedTally.Selection(
                    it.selection_id,
                    it.sequence_order,
                    it.encrypted_vote.import(group),
                )
            })
    }
    return EncryptedTally(this.tally_id, contests)
}