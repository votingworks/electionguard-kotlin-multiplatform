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

fun ChaumPedersenRangeProofKnownNonce.publishJsonE(u_nonces : List<ElementModQ>, c_nonces : List<ElementModQ>) =
    RangeProofJson( this.proofs.mapIndexed { idx, it -> it.publishJsonE(u_nonces[idx], c_nonces[idx]) })

fun RangeProofJson.import(group: GroupContext) = ChaumPedersenRangeProofKnownNonce(this.proofs.map { it.import(group) })

@Serializable
data class ChaumPedersenJson(
    val u_nonce: ElementModQJson, // eq 23
    val c_nonce: ElementModQJson, // eq 24
    val expected_challenge: ElementModQJson,
    val expected_response: ElementModQJson,
)

fun ChaumPedersenProof.publishJsonE(u_nonce : ElementModQ, c_nonce : ElementModQ, ) =
    ChaumPedersenJson(u_nonce.publishJson(), c_nonce.publishJson(), this.c.publishJson(), this.r.publishJson() )

fun ChaumPedersenJson.import(group: GroupContext) =
    ChaumPedersenProof(
        this.expected_challenge.import(group) ?: throw IllegalArgumentException("ChaumPedersenJson malformed expected_challenge"),
        this.expected_response.import(group) ?: throw IllegalArgumentException("ChaumPedersenJson malformed expected_response")
    )
