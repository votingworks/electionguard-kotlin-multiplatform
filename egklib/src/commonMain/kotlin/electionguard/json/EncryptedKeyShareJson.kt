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
    val ownerXcoord : Int,
    val polynomial_owner: String, // guardian j (owns the polynomial Pj)
    val secret_share_for: String, // guardian l
    val encrypted_coordinate: HashedElGamalCiphertextJson,
)

fun EncryptedKeyShare.publish() = EncryptedKeyShareJson(
        this.ownerXcoord,
        this.polynomialOwner,
        this.secretShareFor,
        this.encryptedCoordinate.publish(),
    )

fun EncryptedKeyShareJson.import(group: GroupContext): EncryptedKeyShare? {
    val encryptedCoordinate = this.encrypted_coordinate.import(group)
    return if (encryptedCoordinate == null) null else
        EncryptedKeyShare(
            this.ownerXcoord,
            this.polynomial_owner,
            this.secret_share_for,
            encryptedCoordinate,
        )
}

/** External representation of a KeyShare LOOK */
@Serializable
@SerialName("KeyShare")
data class KeyShareJson(
    val ownerXcoord : Int,
    val polynomial_owner: String, // guardian j (owns the polynomial Pj)
    val secret_share_for: String, // guardian l
    val coordinate: ElementModQJson,
)

fun KeyShare.publish() = KeyShareJson(
    this.ownerXcoord,
    this.polynomialOwner,
    this.secretShareFor,
    this.yCoordinate.publish(),
)

fun KeyShareJson.import(group: GroupContext): KeyShare? {
    val coordinate = this.coordinate.import(group)
    return if (coordinate == null) null else
        KeyShare(
            this.ownerXcoord,
            this.polynomial_owner,
            this.secret_share_for,
            coordinate,
        )
}