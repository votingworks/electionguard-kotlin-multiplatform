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
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrustee
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.keyceremony.PublicKeys
import electionguard.keyceremony.SecretKeyShare

// DecryptingTrustee must stay private. DecryptingGuardian is its public info.
fun GroupContext.importDecryptingTrustee(proto: electionguard.protogen.DecryptingTrustee):
        Result<DecryptingTrustee, String> {

    val id = proto.guardianId
    val electionKeyPair = this.importElGamalKeypair(id, proto.electionKeypair)
    val (shares, serrors) = proto.secretKeyShares.map { this.importSecretKeyShare(id, it) }.partition()
    val (commitments, cerrors) = proto.coefficientCommitments.map { this.importCommitmentSet(id, it) }.partition()

    val errors = getAllErrors(electionKeyPair) + serrors + cerrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    val result = DecryptingTrustee(
        proto.guardianId,
        proto.guardianXCoordinate,
        electionKeyPair.unwrap(),
        shares.associateBy { it.generatingGuardianId },
        commitments.associate { it.guardianId to it.commitments },
    )

    return Ok(result)
}

private fun GroupContext.importElGamalKeypair(id:String, keypair: electionguard.protogen.ElGamalKeypair?):
        Result<ElGamalKeypair, String> {
    if (keypair == null) {
        return Err("DecryptingTrustee $id missing keypair")
    }
    val secretKey = this.importElementModQ(keypair.secretKey)
        .toResultOr {"DecryptingTrustee $id secretKey was malformed or missing"}
    val publicKey = this.importElementModP(keypair.publicKey)
        .toResultOr {"DecryptingTrustee $id publicKey was malformed or missing"}

    val errors = getAllErrors(secretKey, publicKey)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }
    return Ok(ElGamalKeypair(
        ElGamalSecretKey(secretKey.unwrap()),
        ElGamalPublicKey(publicKey.unwrap()),
    ))
}

private fun GroupContext.importSecretKeyShare(id:String, keyShare: electionguard.protogen.SecretKeyShare?):
        Result<SecretKeyShare, String> {
    if (keyShare == null) {
        return Err("DecryptingTrustee $id missing keypair")
    }

    val encryptedCoordinate = this.importHashedCiphertext(keyShare.encryptedCoordinate)
        .toResultOr {"DecryptingTrustee $id secretKey was malformed or missing"}

    val errors = getAllErrors(encryptedCoordinate)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }
    return Ok(SecretKeyShare(
        keyShare.generatingGuardianId,
        keyShare.designatedGuardianId,
        keyShare.designatedGuardianXCoordinate,
        encryptedCoordinate.unwrap(),
    ))
}

private fun GroupContext.importCommitmentSet(id:String, proto: electionguard.protogen.CommitmentSet):
        Result<CommitmentSet, String> {

    val (commitments, cerrors) = proto.commitments.map {
        this.importElementModP(it).toResultOr {"DecryptingTrustee $id commitment was malformed or missing"} }.partition()

    if (cerrors.isNotEmpty()) {
        return Err(cerrors.joinToString("\n"))
    }
    return Ok(CommitmentSet(
        proto.guardianId,
        commitments,
    ))
}

private data class CommitmentSet(val guardianId: String, val commitments: List<ElementModP>)


///////////////////////////////////////////////////////////////////////////////

fun KeyCeremonyTrustee.publishDecryptingTrustee(): electionguard.protogen.DecryptingTrustee {
    return electionguard.protogen.DecryptingTrustee(
        this.id(),
        this.xCoordinate(),
        ElGamalKeypair(
            ElGamalSecretKey(this.electionPrivateKey()),
            ElGamalPublicKey(this.electionPublicKey())
        ).publishElGamalKeyPair(),
        this.otherSharesForMe.values.map { it.publishSecretKeyShare() },
        this.guardianPublicKeys.values.map { it.publishCommitmentSet() },
    )
}

private fun ElGamalKeypair.publishElGamalKeyPair(): electionguard.protogen.ElGamalKeypair {
    return electionguard.protogen.ElGamalKeypair(
        this.secretKey.key.publishElementModQ(),
        this.publicKey.key.publishElementModP(),
    )
}

private fun SecretKeyShare.publishSecretKeyShare(): electionguard.protogen.SecretKeyShare {
    return electionguard.protogen.SecretKeyShare(
        this.generatingGuardianId,
        this.designatedGuardianId,
        this.designatedGuardianXCoordinate,
        this.encryptedCoordinate.publishHashedCiphertext(),
    )
}

private fun PublicKeys.publishCommitmentSet(): electionguard.protogen.CommitmentSet {
    return electionguard.protogen.CommitmentSet(
        this.guardianId,
        this.coefficientCommitments().map { it.publishElementModP() },
    )
}