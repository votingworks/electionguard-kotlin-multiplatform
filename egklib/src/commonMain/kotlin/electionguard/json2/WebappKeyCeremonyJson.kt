package electionguard.json2

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.core.GroupContext
import electionguard.keyceremony.EncryptedKeyShare
import electionguard.keyceremony.KeyShare
import electionguard.keyceremony.PublicKeys
import kotlinx.serialization.Serializable

/** External representation of a PublicKeys, used in KeyCeremony */
@Serializable
data class PublicKeysJson(
    val guardianId: String,
    val guardianXCoordinate: Int,
    val coefficientProofs: List<SchnorrProofJson>,
)

fun PublicKeys.publishJson() = PublicKeysJson(
    this.guardianId,
    this.guardianXCoordinate,
    this.coefficientProofs.map { it.publishJson() }
)

fun PublicKeysJson.import(group: GroupContext) = PublicKeys(
    this.guardianId,
    this.guardianXCoordinate,
    this.coefficientProofs.map { it.import(group) }
)

fun PublicKeysJson.importResult(group: GroupContext): Result<PublicKeys, String> {
    val proofs = this.coefficientProofs.map { it.importResult(group) }
    val allgood = proofs.map { it is Ok }.reduce { a, b -> a && b }

    return if (allgood) Ok( PublicKeys(this.guardianId, this.guardianXCoordinate, proofs.map {it.unwrap()}))
    else Err("PublicKeysJson failed")
}

/////////////////////////////////////////////////////
/** External representation of an EncryptedKeyShare */
@Serializable
data class EncryptedKeyShareJson(
    val ownerXcoord : Int,
    val polynomial_owner: String, // guardian j (owns the polynomial Pj)
    val secret_share_for: String, // guardian l
    val encrypted_coordinate: HashedElGamalCiphertextJson,
)

fun EncryptedKeyShare.publishJson() = EncryptedKeyShareJson(
    this.ownerXcoord,
    this.polynomialOwner,
    this.secretShareFor,
    this.encryptedCoordinate.publishJson(),
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
data class KeyShareJson(
    val ownerXcoord : Int,
    val polynomial_owner: String, // guardian j (owns the polynomial Pj)
    val secret_share_for: String, // guardian l
    val coordinate: ElementModQJson,
)

fun KeyShare.publishJson() = KeyShareJson(
    this.ownerXcoord,
    this.polynomialOwner,
    this.secretShareFor,
    this.yCoordinate.publishJson(),
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
