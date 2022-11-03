package electionguard.json

import electionguard.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** External representation of an GenericChaumPedersenProof */
@Serializable
@SerialName("GenericChaumPedersen")
data class GenericChaumPedersenProofJson(val challenge: ElementModQJson, val response: ElementModQJson)

/** Publishes a [GenericChaumPedersenProof] to its external, serializable form. */
fun GenericChaumPedersenProof.publish() = GenericChaumPedersenProofJson(
    this.c.publishModQ(),
    this.r.publishModQ()
)

/** Imports from a published [DisjunctiveChaumPedersen]. Returns `null` if it's malformed. */
fun GroupContext.importCP(proof: GenericChaumPedersenProofJson): GenericChaumPedersenProof? {
    val c = this.importModQ(proof.challenge)
    val r = this.importModQ(proof.response)
    if (c == null || r == null) return null
    return GenericChaumPedersenProof(c, r)
}

/////////////////////////

/** External representation of an RangeChaumPedersenProofKnownNonce */
@Serializable
@SerialName("RangeChaumPedersenProofKnownNoncePub")
data class RangeChaumPedersenProofKnownNonceJson(
    val proofs: List<GenericChaumPedersenProofJson>,
)

/** Publishes a [RangeChaumPedersenProofKnownNonce] to its external, serializable form. */
fun RangeChaumPedersenProofKnownNonce.publish() = RangeChaumPedersenProofKnownNonceJson(
    this.proofs.map { it.publish() },
)

/** Imports from a published [RangeChaumPedersenProofKnownNonceJson]. Returns `null` if it's malformed. */
fun GroupContext.importRangeCP(proofPub: RangeChaumPedersenProofKnownNonceJson): RangeChaumPedersenProofKnownNonce? {
    return RangeChaumPedersenProofKnownNonce(proofPub.proofs.map { this.importCP(it)!! })
}
