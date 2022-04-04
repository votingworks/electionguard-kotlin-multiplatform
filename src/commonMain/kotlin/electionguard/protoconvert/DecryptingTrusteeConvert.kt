package electionguard.protoconvert

import electionguard.core.ElGamalKeypair
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElGamalSecretKey
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrustee

fun electionguard.protogen.DecryptingTrustee.importDecryptingTrustee(group: GroupContext): DecryptingTrustee {
    return DecryptingTrustee(
        this.guardianId,
        this.guardianXCoordinate,
        this.electionKeyPair!!.importElGamalKeypair(group),
    )
}

private fun electionguard.protogen.ElGamalKeyPair.importElGamalKeypair(group: GroupContext): ElGamalKeypair {
    return ElGamalKeypair(
        ElGamalSecretKey(group.importElementModQ(this.secretKey)!!),
        ElGamalPublicKey(group.importElementModP(this.publicKey)!!),
    )
}