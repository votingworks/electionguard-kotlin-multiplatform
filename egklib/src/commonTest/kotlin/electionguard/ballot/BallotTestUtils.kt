package electionguard.ballot

import com.github.michaelbull.result.unwrap
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.keyceremony.PublicKeys

fun makeDoerreTrustee(ktrustee: KeyCeremonyTrustee): DecryptingTrusteeDoerre {
    return DecryptingTrusteeDoerre(
        ktrustee.id,
        ktrustee.xCoordinate,
        ktrustee.electionPublicKey(),
        ktrustee.computeSecretKeyShare(),
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