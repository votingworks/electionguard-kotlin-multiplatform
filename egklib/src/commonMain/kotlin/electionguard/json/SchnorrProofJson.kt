package electionguard.json

import electionguard.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** External representation of a SchnorrProof */
@Serializable
@SerialName("SchnorrProof")
data class SchnorrProofJson(
    val public_key: ElementModPJson,
    val challenge: ElementModQJson,
    val response: ElementModQJson,
)

/** Publishes a [SchnorrProof] to its external, serializable form. */
fun SchnorrProof.publish() = SchnorrProofJson(
    this.publicKey.publish(),
    this.challenge.publish(),
    this.response.publish(),
)

fun SchnorrProofJson.import(group: GroupContext): SchnorrProof? {
    val p = this.public_key.import(group)
    val c = this.challenge.import(group)
    val r = this.response.import(group)
    return if (p == null || c == null || r == null) null else SchnorrProof(p, c, r)
}