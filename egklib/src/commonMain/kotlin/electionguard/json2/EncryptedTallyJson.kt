package electionguard.json2

import electionguard.ballot.EncryptedTally
import electionguard.core.ElGamalCiphertext
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.util.ErrorMessages
import kotlinx.serialization.Serializable

@Serializable
data class EncryptedTallyJson(
    val tally_id: String,
    val contests: List<EncryptedTallyContestJson>,
    val cast_ballot_ids: List<String>,
    val election_id: UInt256Json,
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
    return EncryptedTallyJson(
        this.tallyId,
        contests,
        this.castBallotIds,
        this.electionId.publishJson(),
        )
}

/////////////////////////////////////////////////////////////////////////////////////////

fun EncryptedTallyJson.import(group: GroupContext, errs : ErrorMessages): EncryptedTally? {
    val contests = this.contests.map { it.import(group, errs.nested("EncryptedTallyContestJson ${it.contest_id}")) }
    val electionId = this.election_id.import() ?: errs.addNull("EncryptedTally malformed election_id") as UInt256?

    return if (errs.hasErrors()) null
    else  EncryptedTally(
        this.tally_id,
        contests.filterNotNull(),
        this.cast_ballot_ids,
        electionId!!,
    )
}

fun EncryptedTallyContestJson.import(group: GroupContext, errs : ErrorMessages): EncryptedTally.Contest? {
    val selections = this.selections.map { it.import(group, errs.nested("EncryptedTallySelectionJson ${it.selection_id}")) }

    return if (errs.hasErrors()) null
    else EncryptedTally.Contest(
        this.contest_id,
        this.sequence_order,
        selections.filterNotNull()
    )
}


fun EncryptedTallySelectionJson.import(group: GroupContext, errs : ErrorMessages): EncryptedTally.Selection? {
    val encryptedVote = this.encrypted_vote.import(group) ?: errs.addNull("malformed encrypted_vote") as ElGamalCiphertext?

    return if (errs.hasErrors()) null
    else EncryptedTally.Selection(
        this.selection_id,
        this.sequence_order,
        encryptedVote!!,
    )

}
