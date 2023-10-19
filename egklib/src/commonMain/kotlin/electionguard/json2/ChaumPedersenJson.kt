package electionguard.json2

import electionguard.core.ChaumPedersenProof
import electionguard.core.ChaumPedersenRangeProofKnownNonce
import electionguard.core.GroupContext
import kotlinx.serialization.Serializable

@Serializable
data class RangeProofJson(
    val proofs: List<ChaumPedersenJson>,
)

fun ChaumPedersenRangeProofKnownNonce.publishJson() =
    RangeProofJson( this.proofs.mapIndexed { idx, it -> it.publishJson() })

fun RangeProofJson.import(group: GroupContext) =
    ChaumPedersenRangeProofKnownNonce(this.proofs.map { it.import(group) })

@Serializable
data class ChaumPedersenJson(
    val challenge: ElementModQJson,
    val response: ElementModQJson,
)

fun ChaumPedersenProof.publishJson() =
    ChaumPedersenJson(this.c.publishJson(), this.r.publishJson())

fun ChaumPedersenJson.import(group: GroupContext) =
    ChaumPedersenProof(this.challenge.import(group), this.response.import(group))