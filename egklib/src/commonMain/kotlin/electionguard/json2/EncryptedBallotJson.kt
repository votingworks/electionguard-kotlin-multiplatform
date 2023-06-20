package electionguard.json2

import electionguard.ballot.EncryptedBallot
import electionguard.core.GroupContext
import electionguard.core.UInt256
import kotlinx.serialization.Serializable

@Serializable
data class EncryptedBallotJson(
    val ballot_id: String,
    val ballot_style_id: String,
    val confirmation_code: UInt256Json,
    val code_baux: String, // Baux in eq 59
    val contests: List<EncryptedContestJson>,
    val timestamp: Long, // Timestamp at which the ballot encryption is generated, in seconds since the epoch UTC.
    val state: String, // BallotState
    val is_preencrypt: Boolean,
    val primary_nonce: UInt256Json?, // only when uncast
)

@Serializable
data class EncryptedContestJson(
    val contest_id: String,
    val sequence_order: Int,
    val contest_hash: UInt256Json,
    val selections: List<EncryptedSelectionJson>,
    val proof: RangeProofJson,
    val encrypted_contest_data: HashedElGamalCiphertextJson,
    val pre_encryption: PreEncryptionJson?, // only for is_preencrypt
)

@Serializable
data class EncryptedSelectionJson(
    val selection_id: String,
    val sequence_order: Int,
    val encrypted_vote: ElGamalCiphertextJson,
    val proof: RangeProofJson,
)

fun EncryptedBallot.publishJson(primaryNonce : UInt256?): EncryptedBallotJson {
    val contests = this.contests.map { econtest ->

        EncryptedContestJson(
            econtest.contestId,
            econtest.sequenceOrder,
            econtest.contestHash.publishJson(),
            econtest.selections.map {
                EncryptedSelectionJson(
                    it.selectionId,
                    it.sequenceOrder,
                    it.encryptedVote.publishJson(),
                    it.proof.publishJson(),
                )
            },
            econtest.proof.publishJson(),
            econtest.contestData.publishJson(),
            econtest.preEncryption?.publishJson(),
        )
    }

    return EncryptedBallotJson(
        this.ballotId,
        this.ballotStyleId,
        this.confirmationCode.publishJson(),
        io.ktor.utils.io.core.String(this.codeBaux),
        contests,
        this.timestamp,
        this.state.name,
        this.isPreencrypt,
        primaryNonce?.publishJson(),
    )
}

@Serializable
data class PreEncryptionJson(
    val preencryption_hash: UInt256Json,
    val all_selection_hashes: List<UInt256Json>,
    val selected_vectors: List<SelectionVectorJson>,
)

fun EncryptedBallot.PreEncryption.publishJson(): PreEncryptionJson {
    return PreEncryptionJson(
        this.preencryptionHash.publishJson(),
        this.allSelectionHashes.map { it.publishJson() },
        this.selectedVectors.map { it.publishJson() },
    )
}

fun PreEncryptionJson.import(group: GroupContext): EncryptedBallot.PreEncryption {
    return EncryptedBallot.PreEncryption(
        this.preencryption_hash.import(),
        this.all_selection_hashes.map { it.import() },
        this.selected_vectors.map { it.import(group) },
    )
}

@Serializable
data class SelectionVectorJson(
    val selection_hash: UInt256Json,
    val short_code : String,
    val encryptions: List<ElGamalCiphertextJson>,
)

fun EncryptedBallot.SelectionVector.publishJson(): SelectionVectorJson {
    return SelectionVectorJson(
        this.selectionHash.publishJson(),
        this.shortCode,
        this.encryptions.map { it.publishJson() },
    )
}

fun SelectionVectorJson.import(group: GroupContext): EncryptedBallot.SelectionVector {
    return EncryptedBallot.SelectionVector(
        this.selection_hash.import(),
        this.short_code,
        this.encryptions.map { it.import(group) },
    )
}
