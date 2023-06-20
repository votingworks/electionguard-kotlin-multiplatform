package electionguard.testvectors

import electionguard.core.GroupContext
import electionguard.core.SchnorrProof
import kotlinx.serialization.Serializable


@Serializable
data class SchnorrProofJson(
    val public_key : ElementModPJson,
    val challenge : ElementModQJson,
    val response : ElementModQJson,
)

fun SchnorrProof.publishJson() = SchnorrProofJson(this.publicKey.publishJson(), this.challenge.publishJson(), this.response.publishJson())
fun SchnorrProofJson.import(group: GroupContext) = SchnorrProof(this.public_key.import(group), this.challenge.import(group), this.response.import(group))