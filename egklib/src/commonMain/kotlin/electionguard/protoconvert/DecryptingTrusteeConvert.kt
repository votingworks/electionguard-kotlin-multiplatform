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
import electionguard.keyceremony.SecretKeyShare

// DecryptingTrustee must stay private. DecryptingGuardian is its public info.
fun GroupContext.importDecryptingTrustee(proto: electionguard.protogen.DecryptingTrustee):
        Result<DecryptingTrustee, String> {

    val id = proto.guardianId
    val electionKeyPair = this.importElGamalKeypair(id, proto.electionKeypair)
    val (shares, serrors) = proto.secretKeyShares.map { this.importSecretKeyShare(id, it) }.partition()

    val errors = getAllErrors(electionKeyPair) + serrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    val result = DecryptingTrustee(
        proto.guardianId,
        proto.guardianXCoordinate,
        electionKeyPair.unwrap(),
        shares.associateBy { it.generatingGuardianId },
    )

    return Ok(result)
}

private fun GroupContext.importElGamalKeypair(id: String, keypair: electionguard.protogen.ElGamalKeypair?):
        Result<ElGamalKeypair, String> {
    if (keypair == null) {
        return Err("DecryptingTrustee $id missing keypair")
    }
    val secretKey = this.importElementModQ(keypair.secretKey)
        .toResultOr { "DecryptingTrustee $id secretKey was malformed or missing" }
    val publicKey = this.importElementModP(keypair.publicKey)
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

private fun GroupContext.importSecretKeyShare(id: String, keyShare: electionguard.protogen.SecretKeyShare?):
        Result<SecretKeyShare, String> {
    if (keyShare == null) {
        return Err("DecryptingTrustee $id missing keypair")
    }

    val encryptedCoordinate = this.importHashedCiphertext(keyShare.encryptedCoordinate)
        .toResultOr { "DecryptingTrustee $id secretKey was malformed or missing" }

    if (encryptedCoordinate is Err) {
        return encryptedCoordinate
    }
    return Ok(
        SecretKeyShare(
            keyShare.generatingGuardianId,
            keyShare.designatedGuardianId,
            keyShare.designatedGuardianXCoordinate,
            encryptedCoordinate.unwrap(),
        )
    )
}


///////////////////////////////////////////////////////////////////////////////

fun KeyCeremonyTrustee.publishDecryptingTrustee() =
    electionguard.protogen.DecryptingTrustee(
        this.id(),
        this.xCoordinate(),
        ElGamalKeypair(
            ElGamalSecretKey(this.electionPrivateKey()),
            ElGamalPublicKey(this.electionPublicKey())
        ).publishElGamalKeyPair(),
        this.otherSharesForMe.values.map { it.publishSecretKeyShare() },
    )

private fun ElGamalKeypair.publishElGamalKeyPair() =
    electionguard.protogen.ElGamalKeypair(
        this.secretKey.key.publishElementModQ(),
        this.publicKey.key.publishElementModP(),
    )

private fun SecretKeyShare.publishSecretKeyShare() =
    electionguard.protogen.SecretKeyShare(
        this.generatingGuardianId,
        this.designatedGuardianId,
        this.designatedGuardianXCoordinate,
        this.encryptedCoordinate.publishHashedCiphertext(),
    )
