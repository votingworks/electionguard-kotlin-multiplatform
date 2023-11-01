package electionguard.json2

import electionguard.core.*
import electionguard.util.ErrorMessages
import kotlinx.serialization.Serializable

@Serializable
data class RangeProofJson(
    val proofs: List<ChaumPedersenJson>,
)

fun ChaumPedersenRangeProofKnownNonce.publishJson() =
    RangeProofJson( this.proofs.map { it.publishJson() })

fun RangeProofJson.import(group: GroupContext, errs: ErrorMessages) : ChaumPedersenRangeProofKnownNonce? {
    val proofs = this.proofs.mapIndexed { idx, it -> it.import(group, errs.nested("Proof $idx")) }
    return if (errs.hasErrors()) null else ChaumPedersenRangeProofKnownNonce(proofs.filterNotNull())
}

@Serializable
data class ChaumPedersenJson(
    val challenge: ElementModQJson,
    val response: ElementModQJson,
)

fun ChaumPedersenProof.publishJson() =
    ChaumPedersenJson(this.c.publishJson(), this.r.publishJson())

fun ChaumPedersenJson.import(group: GroupContext, errs: ErrorMessages = ErrorMessages("ChaumPedersenJson.import")): ChaumPedersenProof? {
    val challenge = this.challenge.import(group) ?: errs.addNull("malformed challenge") as ElementModQ?
    val response = this.response.import(group) ?: errs.addNull("malformed response") as ElementModQ?

    return if (errs.hasErrors()) null else ChaumPedersenProof(challenge!!, response!!)
}