package electionguard.json

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.core.*
import electionguard.keyceremony.PublicKeys
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** External representation of a PublicKeys, used in KeyCeremony */
@Serializable
@SerialName("PublicKeys")
data class PublicKeysJson(
    val guardianId: String,
    val guardianXCoordinate: Int,
    val coefficientProofs: List<SchnorrProofJson>,
)

fun PublicKeys.publish() = PublicKeysJson(
        this.guardianId,
        this.guardianXCoordinate,
        this.coefficientProofs.map { it.publish() }
    )

fun GroupContext.importPublicKeys(proof: PublicKeysJson): Result<PublicKeys, String> {
    val proofs = proof.coefficientProofs.map { this.importSchnorrProof(it) }
    val allgood = proofs.map { it != null }.reduce { a, b -> a && b }

    return if (allgood) Ok(PublicKeys(proof.guardianId, proof.guardianXCoordinate, proofs.map {it!!}))
    else Err("importSchnorrProof failed")
}