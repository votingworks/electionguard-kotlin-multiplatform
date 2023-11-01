package electionguard.json2

import electionguard.ballot.EncryptedBallot
import electionguard.core.*
import electionguard.core.Base16.fromHex
import electionguard.core.Base16.fromHexSafe
import electionguard.core.Base16.toHex
import electionguard.util.ErrorMessages
import kotlinx.serialization.Serializable

@Serializable
data class EncryptedBallotJson(
    val ballot_id: String,
    val ballot_style_id: String,
    val encrypting_device: String,
    val timestamp: Long, // Timestamp at which the ballot encryption is generated, in seconds since the epoch UTC.
    val code_baux: String, // Baux in eq 59
    val confirmation_code: UInt256Json,
    val election_id: UInt256Json,
    val contests: List<EncryptedContestJson>,
    val state: String, // BallotState
    val is_preencrypt: Boolean,
    val primary_nonce: UInt256Json?, // only when uncast
)

@Serializable
data class EncryptedContestJson(
    val contest_id: String,
    val sequence_order: Int,
    val votes_allowed: Int,
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

fun EncryptedBallot.publishJson(primaryNonce : UInt256? = null): EncryptedBallotJson {
    val contests = this.contests.map { econtest ->

        EncryptedContestJson(
            econtest.contestId,
            econtest.sequenceOrder,
            econtest.votesAllowed,
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
        this.encryptingDevice,
        this.timestamp,
        this.codeBaux.toHex(),
        this.confirmationCode.publishJson(),
        this.electionId.publishJson(),
        contests,
        this.state.name,
        this.isPreencrypt,
        primaryNonce?.publishJson(),
    )
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

fun EncryptedBallotJson.import(group : GroupContext, errs : ErrorMessages): EncryptedBallot? {
    val confirmationCode = this.confirmation_code.import() ?: errs.addNull("malformed confirmation_code") as UInt256?
    val electionId = this.election_id.import() ?: errs.addNull("malformed election_id") as UInt256?
    val contests = this.contests.map { it.import(group, errs.nested("Contest ${it.contest_id}")) }
    val state = safeEnumValueOf<EncryptedBallot.BallotState>(this.state) ?: errs.addNull("malformed BallotState") as EncryptedBallot.BallotState?
    val baux = this.code_baux.fromHex() ?: errs.addNull("malformed baux") as ByteArray?

    return if (errs.hasErrors()) null
    else EncryptedBallot(
        this.ballot_id,
        this.ballot_style_id,
        this.encrypting_device,
        this.timestamp,
        baux!!,
        confirmationCode!!,
        electionId!!,
        contests.filterNotNull(),
        state!!,
        this.is_preencrypt,
    )
}

fun EncryptedContestJson.import(group : GroupContext, errs : ErrorMessages): EncryptedBallot.Contest? {
    val contestHash = this.contest_hash.import() ?: errs.addNull("malformed contest_hash") as UInt256?
    val encryptedContestData = this.encrypted_contest_data.import(group) ?: errs.addNull("malformed encrypted_contest_data") as HashedElGamalCiphertext?
    val selections = this.selections.map { it.import(group, errs.nested("Selection ${it.selection_id}")) }
    val preEncryption = if (this.pre_encryption == null) null else this.pre_encryption.import(group, errs.nested("PreEncryption"))
    val proof = this.proof.import(group, errs.nested("Proof"))

    return if (errs.hasErrors()) null
    else EncryptedBallot.Contest(
        this.contest_id,
        this.sequence_order,
        this.votes_allowed,
        contestHash!!,
        selections.filterNotNull(),
        proof!!,
        encryptedContestData!!,
        preEncryption,
    )
}

fun EncryptedSelectionJson.import(group : GroupContext, errs : ErrorMessages): EncryptedBallot.Selection? {
    val encryptedVote = this.encrypted_vote.import(group) ?: errs.addNull("malformed encrypted_vote") as ElGamalCiphertext?
    val proof = this.proof.import(group, errs.nested("Proof"))

    return if (errs.hasErrors()) null
    else EncryptedBallot.Selection(
        this.selection_id,
        this.sequence_order,
        encryptedVote!!,
        proof!!,
    )
}

@Serializable
data class PreEncryptionJson(
    val preencryption_hash: UInt256Json,
    val all_selection_hashes: List<UInt256Json>, // size = nselections + limit, sorted numerically
    val selected_vectors: List<SelectionVectorJson>, // size = limit, sorted numerically
)

fun EncryptedBallot.PreEncryption.publishJson(): PreEncryptionJson {
    return PreEncryptionJson(
        this.preencryptionHash.publishJson(),
        this.allSelectionHashes.map { it.publishJson() },
        this.selectedVectors.map { it.publishJson() },
    )
}

fun PreEncryptionJson.import(group: GroupContext, errs: ErrorMessages): EncryptedBallot.PreEncryption? {
    val preencryptionHash = this.preencryption_hash.import() ?: errs.addNull("malformed preencryption_hash") as UInt256?
    val allSelectionHashes = this.all_selection_hashes.mapIndexed { idx, it -> it.import() ?: errs.addNull("malformed all_selection_hashes $idx") as UInt256? }
    val selectedVectors = this.selected_vectors.mapIndexed { idx,it -> it.import(group, errs.nested("selectedVectors $idx")) }

    return if (errs.hasErrors()) null
    else  EncryptedBallot.PreEncryption(
        preencryptionHash!!,
        allSelectionHashes.filterNotNull(),
        selectedVectors.filterNotNull(),
    )
}

@Serializable
data class SelectionVectorJson(
    val selection_hash: UInt256Json,
    val short_code : String,
    val encryptions: List<ElGamalCiphertextJson>, // Ej, size = nselections, in order by sequence_order
)

fun EncryptedBallot.SelectionVector.publishJson(): SelectionVectorJson {
    return SelectionVectorJson(
        this.selectionHash.publishJson(),
        this.shortCode,
        this.encryptions.map { it.publishJson() },
    )
}

fun SelectionVectorJson.import(group: GroupContext, errs: ErrorMessages): EncryptedBallot.SelectionVector? {
    val selection_hash = this.selection_hash.import() ?: errs.addNull("malformed selection_hash") as UInt256?
    val encryptions = this.encryptions.mapIndexed { idx, it -> it.import(group)  ?: errs.addNull("malformed encryption $idx") as ElGamalCiphertext? }

    return if (errs.hasErrors()) null
    else  EncryptedBallot.SelectionVector(
        selection_hash!!,
        this.short_code,
        encryptions.filterNotNull(),
    )
}
