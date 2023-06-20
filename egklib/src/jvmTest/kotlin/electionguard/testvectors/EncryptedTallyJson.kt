package electionguard.testvectors

import electionguard.ballot.EncryptedTally
import electionguard.core.GroupContext
import kotlinx.serialization.Serializable

@Serializable
data class EncryptedTallyJson(
    val contests: List<EncryptedTallyContestJson>,
)

@Serializable
data class EncryptedTallyContestJson(
    val contestId: String,
    val sequenceOrder: Int,
    val selections: List<EncryptedTallySelectionJson>,
)

@Serializable
data class EncryptedTallySelectionJson(
    val selectionId: String,
    val sequenceOrder: Int,
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
                    it.ciphertext.publishJson(),
                )
            })
    }
    return EncryptedTallyJson(contests)
}

fun EncryptedTallyJson.import(group: GroupContext): EncryptedTally {
    val contests = this.contests.map { pcontest ->

        EncryptedTally.Contest(
            pcontest.contestId,
            pcontest.sequenceOrder,
            pcontest.selections.map {
                EncryptedTally.Selection(
                    it.selectionId,
                    it.sequenceOrder,
                    it.encrypted_vote.import(group),
                )
            })
    }
    return EncryptedTally("tallyId", contests)
}