package electionguard.json

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** External representation of a GenericChaumPedersenProof */
@Serializable
@SerialName("ChaumPedersenProof")
data class ChaumPedersenProofJson(
    val challenge: ElementModQJson,
    val response: ElementModQJson
    )

fun ChaumPedersenProof.publish() = ChaumPedersenProofJson(
    this.c.publish(),
    this.r.publish()
)

fun ChaumPedersenProofJson.import(group: GroupContext): ChaumPedersenProof? {
    val c = this.challenge.import(group)
    val r = this.response.import(group)
    if (c == null || r == null) return null
    return ChaumPedersenProof(c, r)
}

/////////////////////////

/** External representation of a RangeChaumPedersenProofKnownNonce */
@Serializable
@SerialName("RangeProof")
data class RangeProofJson(
    val proofs: List<ChaumPedersenProofJson>,
)

fun ChaumPedersenRangeProofKnownNonce.publish() = RangeProofJson(
    this.proofs.map { it.publish() },
)

fun RangeProofJson.import(group: GroupContext): Result<ChaumPedersenRangeProofKnownNonce, String> {
    val proofs = this.proofs.map { it.import(group) }
    val allgood = proofs.map { it != null }.reduce { a, b -> a && b }

    return if (allgood) Ok(ChaumPedersenRangeProofKnownNonce(proofs.map { it!!} ))
    else Err("importChaumPedersenProof failed")
}
