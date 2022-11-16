package electionguard.json

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** External representation of a GenericChaumPedersenProof */
@Serializable
@SerialName("GenericChaumPedersen")
data class GenericChaumPedersenProofJson(val challenge: ElementModQJson, val response: ElementModQJson)

fun GenericChaumPedersenProof.publish() = GenericChaumPedersenProofJson(
    this.c.publishModQ(),
    this.r.publishModQ()
)

fun GroupContext.importCP(proof: GenericChaumPedersenProofJson): GenericChaumPedersenProof? {
    val c = this.importModQ(proof.challenge)
    val r = this.importModQ(proof.response)
    if (c == null || r == null) return null
    return GenericChaumPedersenProof(c, r)
}

/////////////////////////

/** External representation of a RangeChaumPedersenProofKnownNonce */
@Serializable
@SerialName("RangeChaumPedersenProofKnownNoncePub")
data class RangeChaumPedersenProofKnownNonceJson(
    val proofs: List<GenericChaumPedersenProofJson>,
)

fun RangeChaumPedersenProofKnownNonce.publish() = RangeChaumPedersenProofKnownNonceJson(
    this.proofs.map { it.publish() },
)

fun GroupContext.importRangeCP(range: RangeChaumPedersenProofKnownNonceJson): Result<RangeChaumPedersenProofKnownNonce, String> {
    val proofs = range.proofs.map { this.importCP(it) }
    val allgood = proofs.map { it != null }.reduce { a, b -> a && b }

    return if (allgood) Ok(RangeChaumPedersenProofKnownNonce(proofs.map { it!!} ))
    else Err("importChaumPedersenProof failed")
}
