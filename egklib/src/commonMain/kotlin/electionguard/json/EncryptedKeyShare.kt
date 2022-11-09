package electionguard.json

import electionguard.core.*
import electionguard.keyceremony.PublicKeys
import electionguard.keyceremony.EncryptedKeyShare
import electionguard.keyceremony.KeyShare
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** External representation of an EncryptedKeyShare */
@Serializable
@SerialName("EncryptedKeyShare")
data class EncryptedKeyShareJson(
    val missingGuardianId: String, // guardian j (owns the polynomial Pj)
    val availableGuardianId: String, // guardian l
    val encryptedCoordinate: HashedElGamalCiphertextJson,
)

/** Publishes a [PublicKeys] to its external, serializable form. */
fun EncryptedKeyShare.publish() = EncryptedKeyShareJson(
    this.missingGuardianId,
    this.availableGuardianId,
    this.encryptedCoordinate.publish(),
)

/** Imports from a published [PublicKeys]. Returns `null` if it's malformed. */
fun GroupContext.importSecretKeyShare(json: EncryptedKeyShareJson): EncryptedKeyShare {
    return EncryptedKeyShare(
        json.missingGuardianId,
        json.availableGuardianId,
        this.importHashedElGamalCiphertext(json.encryptedCoordinate)!!,
    )
}

/** External representation of an KeyShare */
@Serializable
@SerialName("KeyShare")
data class KeyShareJson(
    val missingGuardianId: String, // guardian j (owns the polynomial Pj)
    val availableGuardianId: String, // guardian l
    val coordinate: ElementModQJson,
    val nonce: ElementModQJson,
)

/** Publishes a [PublicKeys] to its external, serializable form. */
fun KeyShare.publish() = KeyShareJson(
    this.missingGuardianId,
    this.availableGuardianId,
    this.coordinate.publishModQ(),
    this.nonce.publishModQ(),
)

/** Imports from a published [PublicKeys]. Returns `null` if it's malformed. */
fun GroupContext.importKeyShare(json: KeyShareJson): KeyShare {
    return KeyShare(
        json.missingGuardianId,
        json.availableGuardianId,
        this.importModQ(json.coordinate)!!,
        this.importModQ(json.nonce)!!,
    )
}