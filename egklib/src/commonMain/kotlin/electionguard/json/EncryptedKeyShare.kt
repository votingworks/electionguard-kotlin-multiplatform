package electionguard.json

import electionguard.core.*
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

fun EncryptedKeyShare.publish() = EncryptedKeyShareJson(
        this.missingGuardianId,
        this.availableGuardianId,
        this.encryptedCoordinate.publish(),
    )

fun GroupContext.importEncryptedKeyShare(json: EncryptedKeyShareJson): EncryptedKeyShare? {
    val encryptedCoordinate = this.importHashedElGamalCiphertext(json.encryptedCoordinate)
    return if (encryptedCoordinate == null) null else
        EncryptedKeyShare(
            json.missingGuardianId,
            json.availableGuardianId,
            encryptedCoordinate,
        )
}

/** External representation of a KeyShare */
@Serializable
@SerialName("KeyShare")
data class KeyShareJson(
    val missingGuardianId: String, // guardian j (owns the polynomial Pj)
    val availableGuardianId: String, // guardian l
    val coordinate: ElementModQJson,
    val nonce: ElementModQJson,
)

fun KeyShare.publish() = KeyShareJson(
        this.missingGuardianId,
        this.availableGuardianId,
        this.coordinate.publishModQ(),
        this.nonce.publishModQ(),
    )

fun GroupContext.importKeyShare(json: KeyShareJson): KeyShare? {
    val coordinate = this.importModQ(json.coordinate)
    val nonce = this.importModQ(json.nonce)
    return if (coordinate == null || nonce == null) null else
        KeyShare(
            json.missingGuardianId,
            json.availableGuardianId,
            coordinate,
            nonce,
        )
}