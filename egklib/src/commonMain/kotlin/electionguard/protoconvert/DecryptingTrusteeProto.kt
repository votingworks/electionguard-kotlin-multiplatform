package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.partition
import com.github.michaelbull.result.toResultOr
import com.github.michaelbull.result.unwrap
import electionguard.core.ElGamalKeypair
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElGamalSecretKey
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrustee
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.keyceremony.EncryptedKeyShare

fun electionguard.protogen.DecryptingTrustee.import(group: GroupContext):
        Result<DecryptingTrustee, String> {

    val id = this.guardianId
    val electionKeyPair = this.electionKeypair?.import(id, group) ?: Err("DecryptingTrustee $id missing keypair")
    val (shares, serrors) = this.secretKeyShares.map { it.import(id, group) }.partition()

    val errors = getAllErrors(electionKeyPair) + serrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    val result = DecryptingTrustee(
        this.guardianId,
        this.guardianXCoordinate,
        electionKeyPair.unwrap(),
        shares.associateBy { it.missingGuardianId },
    )

    return Ok(result)
}

private fun electionguard.protogen.ElGamalKeypair.import(id: String, group: GroupContext):
        Result<ElGamalKeypair, String> {

    val secretKey = group.importElementModQ(this.secretKey)
        .toResultOr { "DecryptingTrustee $id secretKey was malformed or missing" }
    val publicKey = group.importElementModP(this.publicKey)
        .toResultOr { "DecryptingTrustee $id publicKey was malformed or missing" }

    val errors = getAllErrors(secretKey, publicKey)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }
    return Ok(
        ElGamalKeypair(
            ElGamalSecretKey(secretKey.unwrap()),
            ElGamalPublicKey(publicKey.unwrap()),
        )
    )
}

private fun electionguard.protogen.EncryptedKeyShare.import(id: String, group: GroupContext):
        Result<EncryptedKeyShare, String> {

    val encryptedCoordinate = group.importHashedCiphertext(this.encryptedCoordinate)
        .toResultOr { "DecryptingTrustee $id secretKey was malformed or missing" }

    if (encryptedCoordinate is Err) {
        return encryptedCoordinate
    }
    return Ok(
        EncryptedKeyShare(
            this.generatingGuardianId,
            this.designatedGuardianId,
            encryptedCoordinate.unwrap(),
        )
    )
}

///////////////////////////////////////////////////////////////////////////////

fun KeyCeremonyTrustee.publishDecryptingTrusteeProto() =
    electionguard.protogen.DecryptingTrustee(
        this.id(),
        this.xCoordinate(),
        ElGamalKeypair(
            ElGamalSecretKey(this.electionPrivateKey()),
            ElGamalPublicKey(this.electionPublicKey())
        ).publishProto(),
        this.myShareOfOthers.values.map { it.publishProto() },
    )

private fun ElGamalKeypair.publishProto() =
    electionguard.protogen.ElGamalKeypair(
        this.secretKey.key.publishProto(),
        this.publicKey.key.publishProto(),
    )

private fun EncryptedKeyShare.publishProto() =
    electionguard.protogen.EncryptedKeyShare(
        this.missingGuardianId,
        this.availableGuardianId,
        this.encryptedCoordinate.publishProto(),
    )
