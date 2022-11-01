package electionguard.json

import electionguard.core.*
import electionguard.keyceremony.PublicKeys
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** External representation of an PublicKeys */
@Serializable
@SerialName("PublicKeys")
data class PublicKeysJson(
    val guardianId: String,
    val guardianXCoordinate: Int,
    val coefficientProofs: List<SchnorrProofJson>,
)

/** Publishes a [PublicKeys] to its external, serializable form. */
fun PublicKeys.publish() = PublicKeysJson(
    this.guardianId,
    this.guardianXCoordinate,
    this.coefficientProofs.map { it.publish() }
)

/** Imports from a published [PublicKeys]. Returns `null` if it's malformed. */
fun GroupContext.importPublicKeys(proof: PublicKeysJson): PublicKeys? {
    return PublicKeys(
        proof.guardianId,
        proof.guardianXCoordinate,
        proof.coefficientProofs.map { this.importSchnorrProof(it)!! } ,
    )
}