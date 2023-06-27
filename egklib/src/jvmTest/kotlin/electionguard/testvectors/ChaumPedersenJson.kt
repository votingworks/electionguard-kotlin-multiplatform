package electionguard.testvectors

import electionguard.core.ChaumPedersenProof
import electionguard.core.ChaumPedersenRangeProofKnownNonce
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.json2.*
import kotlinx.serialization.Serializable

@Serializable
data class RangeProofJson(
    val proofs: List<ChaumPedersenJson>,
)

fun ChaumPedersenRangeProofKnownNonce.publishJson(u_nonces : List<ElementModQ>, c_nonces : List<ElementModQ>) =
    RangeProofJson( this.proofs.mapIndexed { idx, it -> it.publishJson(u_nonces[idx], c_nonces[idx]) })
fun RangeProofJson.import(group: GroupContext) = ChaumPedersenRangeProofKnownNonce(this.proofs.map { it.import(group) })

@Serializable
data class ChaumPedersenJson(
    val u_nonce: ElementModQJson, // eq 23
    val c_nonce: ElementModQJson, // eq 24
    val expected_challenge: ElementModQJson,
    val expected_response: ElementModQJson,
)

fun ChaumPedersenProof.publishJson(u_nonce : ElementModQ, c_nonce : ElementModQ, ) =
    ChaumPedersenJson(u_nonce.publishJson(), c_nonce.publishJson(), this.c.publishJson(), this.r.publishJson(), )
fun ChaumPedersenJson.import(group: GroupContext) =
    ChaumPedersenProof(this.expected_challenge.import(group), this.expected_response.import(group))