package electionguard.ballot

import com.github.michaelbull.result.unwrap
import electionguard.core.ElGamalKeypair
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElGamalSecretKey
import electionguard.decrypt.DecryptingTrustee
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.keyceremony.PublicKeys

fun makeDecryptingTrustee(ktrustee: KeyCeremonyTrustee): DecryptingTrustee {
    return DecryptingTrustee(
        ktrustee.id,
        ktrustee.xCoordinate,
        ElGamalKeypair(
            ElGamalSecretKey(ktrustee.electionPrivateKey()),
            ElGamalPublicKey(ktrustee.electionPublicKey())
        ),
        ktrustee.otherSharesForMe,
    )
}

fun makeGuardian(trustee: KeyCeremonyTrustee): Guardian {
    val publicKeys = trustee.publicKeys().unwrap()
    return Guardian(
        trustee.id,
        trustee.xCoordinate,
        publicKeys.coefficientProofs,
    )
}

fun makeGuardian(publicKeys: PublicKeys): Guardian {
    return Guardian(
        publicKeys.guardianId,
        publicKeys.guardianXCoordinate,
        publicKeys.coefficientProofs,
    )
}