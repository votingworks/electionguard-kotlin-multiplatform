package electionguard.json

import electionguard.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** External representation of an SchnorrProof */
@Serializable
@SerialName("SchnorrProof")
data class SchnorrProofJson(
    val public_key: ElementModPJson,
    val challenge: ElementModQJson,
    val response: ElementModQJson,
)

/** Publishes a [SchnorrProof] to its external, serializable form. */
fun SchnorrProof.publish() = SchnorrProofJson(
    this.publicKey.publishModP(),
    this.challenge.publishModQ(),
    this.response.publishModQ(),
)

/** Imports from a published [SchnorrProof]. Returns `null` if it's malformed. */
fun GroupContext.importSchnorrProof(proof: SchnorrProofJson): SchnorrProof? {
    val p = this.importModP(proof.public_key)
    val c = this.importModQ(proof.challenge)
    val r = this.importModQ(proof.response)
    return if (p == null || c == null || r == null) null else SchnorrProof(p, c, r)
}