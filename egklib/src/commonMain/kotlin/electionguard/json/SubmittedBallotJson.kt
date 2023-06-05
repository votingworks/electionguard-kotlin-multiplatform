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
    val code: UInt256Json,
    val contests: List<SubmittedContestJson>,
    val timestamp: Long,
    val state: String,
    val is_preencrypt: Boolean
)

//     val ballotId: String,
//    val ballotStyleId: String,  // matches a Manifest.BallotStyle
//    val manifestHash: UInt256,  // matches Manifest.manifestHash
//    val confirmationCode: UInt256, // tracking code, H(B), eq 59
//    val contests: List<Contest>,
//    val timestamp: Long,
//    val state: BallotState,
//    val isPreencrypt: Boolean = false
fun EncryptedBallot.publish() = SubmittedBallotJson(
        this.ballotId,
        this.ballotStyleId,
        this.confirmationCode.publish(),
        this.contests.map { it.publish() },
        this.timestamp,
        this.state.name,
        this.isPreencrypt,
    )

fun SubmittedBallotJson.import(group: GroupContext) : Result<EncryptedBallot, String> {
    val code = this.code.import()
    if (code == null) {
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
        code,
        ByteArray(0),
        contests,
        this.timestamp,
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
    val contest_hash: UInt256Json,
    val ballot_selections: List<SubmittedSelectionJson>,
    val proof: RangeProofJson,
    val contest_data: HashedElGamalCiphertextJson,
)

fun EncryptedBallot.Contest.publish() = SubmittedContestJson(
    this.contestId,
    this.sequenceOrder,
    this.contestHash.publish(),
    this.selections.map { it.publish() },
    this.proof.publish(),
    this.contestData.publish(),
)

fun SubmittedContestJson.import(group: GroupContext) : Result<EncryptedBallot.Contest, String> {
    val contestHash = this.contest_hash.import()
    val contestData = this.contest_data.import(group)
    if (contestHash == null || contestData == null) {
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
    val ciphertext: ElGamalCiphertextJson,
    val proof: RangeProofJson,
)

fun EncryptedBallot.Selection.publish() = SubmittedSelectionJson(
    this.selectionId,
    this.sequenceOrder,
    this.ciphertext.publish(),
    this.proof.publish(),
)

fun SubmittedSelectionJson.import(group: GroupContext) : Result<EncryptedBallot.Selection, String> {
    val ciphertext = this.ciphertext.import(group)
    if (ciphertext == null) {
        return Err("Failed to import selection ${this.object_id}")
    }
    val proof = this.proof.import(group)
    if (proof is Err) {
        return Err(proof.error)
    }
    return Ok(EncryptedBallot.Selection(
        this.object_id,
        this.sequence_order,
        ciphertext,
        proof.unwrap(),
    ))
}
