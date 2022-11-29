package electionguard.json

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.partition
import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedBallot
import electionguard.core.GroupContext
import electionguard.core.safeEnumValueOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("SubmittedBallot")
data class SubmittedBallotJson(
    val object_id: String,
    val style_id: String,
    val manifest_hash: UInt256Json,
    val code_seed: UInt256Json,
    val contests: List<SubmittedContestJson>,
    val code: UInt256Json,
    val timestamp: Long,
    val crypto_hash: UInt256Json,
    val state: String,
    val is_preencrypt: Boolean
)

fun EncryptedBallot.publish() = SubmittedBallotJson(
        this.ballotId,
        this.ballotStyleId,
        this.manifestHash.publish(),
        this.codeSeed.publish(),
        this.contests.map { it.publish() },
        this.code.publish(),
        this.timestamp,
        this.cryptoHash.publish(),
        this.state.name,
        this.isPreencrypt,
    )

fun SubmittedBallotJson.import(group: GroupContext) : Result<EncryptedBallot, String> {
    val manifestHash = this.manifest_hash.import()
    val cryptoHash = this.crypto_hash.import()
    val codeSeed = this.code_seed.import()
    val code = this.code.import()
    if (manifestHash == null || cryptoHash == null || codeSeed == null || code == null) {
        return Err("Failed to import manifest ${this.object_id}")
    }
    val (contests, cerrors) = this.contests.map { it.import(group) }.partition()
    if (cerrors.isNotEmpty()) {
        return Err(cerrors.joinToString("\n"))
    }
    val state = safeEnumValueOf(this.state) ?: EncryptedBallot.BallotState.UNKNOWN
    return Ok(EncryptedBallot(
        this.object_id,
        this.style_id,
        manifestHash,
        codeSeed,
        code,
        contests,
        this.timestamp,
        cryptoHash,
        state,
        this.is_preencrypt,
    ))
}

/////////////////////////

@Serializable
@SerialName("SubmittedContest")
data class SubmittedContestJson(
    val object_id: String,
    val sequence_order: Int,
    val description_hash: UInt256Json,
    val ballot_selections: List<SubmittedSelectionJson>,
    val crypto_hash: UInt256Json,
    val proof: RangeProofJson,
    val contest_data: HashedElGamalCiphertextJson,
)

fun EncryptedBallot.Contest.publish() = SubmittedContestJson(
    this.contestId,
    this.sequenceOrder,
    this.contestHash.publish(),
    this.selections.map { it.publish() },
    this.cryptoHash.publish(),
    this.proof.publish(),
    this.contestData.publish(),
)

fun SubmittedContestJson.import(group: GroupContext) : Result<EncryptedBallot.Contest, String> {
    val contestHash = this.description_hash.import()
    val cryptoHash = this.crypto_hash.import()
    val contestData = this.contest_data.import(group)
    if (contestHash == null || cryptoHash == null || contestData == null) {
        return Err("Failed to import contest ${this.object_id}")
    }
    val (selections, serrors) = this.ballot_selections.map { it.import(group) }.partition()
    val proof = this.proof.import(group)
    val errors = getAllErrors(proof) + serrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }
    return Ok(EncryptedBallot.Contest(
        this.object_id,
        this.sequence_order,
        contestHash,
        selections,
        cryptoHash,
        proof.unwrap(),
        contestData,
    ))
}

/////////////////////////

@Serializable
@SerialName("SubmittedSelection")
data class SubmittedSelectionJson(
    val object_id: String,
    val sequence_order: Int,
    val description_hash: UInt256Json,
    val ciphertext: ElGamalCiphertextJson,
    val crypto_hash: UInt256Json,
    val proof: RangeProofJson,
)

fun EncryptedBallot.Selection.publish() = SubmittedSelectionJson(
    this.selectionId,
    this.sequenceOrder,
    this.selectionHash.publish(),
    this.ciphertext.publish(),
    this.cryptoHash.publish(),
    this.proof.publish(),
)

fun SubmittedSelectionJson.import(group: GroupContext) : Result<EncryptedBallot.Selection, String> {
    val selectionHash = this.description_hash.import()
    val ciphertext = this.ciphertext.import(group)
    val cryptoHash = this.crypto_hash.import()
    if (selectionHash == null || ciphertext == null || cryptoHash == null) {
        return Err("Failed to import selection ${this.object_id}")
    }
    val proof = this.proof.import(group)
    if (proof is Err) {
        return Err(proof.error)
    }
    return Ok(EncryptedBallot.Selection(
        this.object_id,
        this.sequence_order,
        selectionHash,
        ciphertext,
        cryptoHash,
        proof.unwrap(),
    ))
}
