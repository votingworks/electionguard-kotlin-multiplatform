package electionguard.keyceremony

import com.github.michaelbull.result.Result
import electionguard.core.ElementModP
import electionguard.core.SchnorrProof

interface KeyCeremonyTrusteeIF {
    fun id(): String
    fun xCoordinate(): Int
    fun electionPublicKey(): ElementModP
    fun coefficientCommitments(): List<ElementModP>
    fun coefficientProofs(): List<SchnorrProof>

    /** Send my PublicKeys. */
    fun publicKeys(): Result<PublicKeys, String>
    /** Receive the PublicKeys from another guardian. */
    fun receivePublicKeys(publicKeys: PublicKeys): Result<Boolean, String>
    /** Create my SecretKeyShare for another guardian. */
    fun secretKeyShareFor(otherGuardian: String): Result<SecretKeyShare, String>
    /** Receive and verify another guardian's SecretKeyShare for me. */
    fun receiveSecretKeyShare(share: SecretKeyShare): Result<Boolean, String>
}