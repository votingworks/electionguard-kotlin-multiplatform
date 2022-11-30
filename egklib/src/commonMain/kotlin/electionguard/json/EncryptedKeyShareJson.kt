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

fun EncryptedKeyShareJson.import(group: GroupContext): EncryptedKeyShare? {
    val encryptedCoordinate = this.encryptedCoordinate.import(group)
    return if (encryptedCoordinate == null) null else
        EncryptedKeyShare(
            this.missingGuardianId,
            this.availableGuardianId,
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
        this.coordinate.publish(),
        this.nonce.publish(),
    )

fun KeyShareJson.import(group: GroupContext): KeyShare? {
    val coordinate = this.coordinate.import(group)
    val nonce = this.nonce.import(group)
    return if (coordinate == null || nonce == null) null else
        KeyShare(
            this.missingGuardianId,
            this.availableGuardianId,
            coordinate,
            nonce,
        )
}