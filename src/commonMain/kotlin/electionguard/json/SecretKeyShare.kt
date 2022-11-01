package electionguard.json

import electionguard.core.*
import electionguard.keyceremony.PublicKeys
import electionguard.keyceremony.SecretKeyShare
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** External representation of an SecretKeyShare */
@Serializable
@SerialName("SecretKeyShare")
data class SecretKeyShareJson(
    val generatingGuardianId: String, // guardian j (missing)
    val designatedGuardianId: String, // guardian l
    val designatedGuardianXCoordinate: Int,  // ℓ
    val encryptedCoordinate: HashedElGamalCiphertextJson,
)

/** Publishes a [PublicKeys] to its external, serializable form. */
fun SecretKeyShare.publish() = SecretKeyShareJson(
    this.generatingGuardianId,
    this.designatedGuardianId,
    this.designatedGuardianXCoordinate,
    this.encryptedCoordinate.publish(),
)

/** Imports from a published [PublicKeys]. Returns `null` if it's malformed. */
fun GroupContext.importSecretKeyShare(proof: SecretKeyShareJson): SecretKeyShare? {
    return SecretKeyShare(
        proof.generatingGuardianId,
        proof.designatedGuardianId,
        proof.designatedGuardianXCoordinate,
        this.importHashedElGamalCiphertext(proof.encryptedCoordinate)!!,
    )
}