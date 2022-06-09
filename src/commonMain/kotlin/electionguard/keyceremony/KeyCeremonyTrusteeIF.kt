package electionguard.keyceremony

import com.github.michaelbull.result.Result
import electionguard.core.ElementModP

interface KeyCeremonyTrusteeIF {
    fun id(): String
    fun xCoordinate(): Int
    fun electionPublicKey(): ElementModP
    fun coefficientCommitments(): List<ElementModP>

    /** Send my PublicKeys. */
    fun sendPublicKeys(): Result<PublicKeys, String>
    /** Receive the PublicKeys from another guardian. */
    fun receivePublicKeys(publicKeys: PublicKeys): Result<PublicKeys, String>
    /** Create my SecretKeyShare for another guardian. */
    fun sendSecretKeyShare(otherGuardian: String): Result<SecretKeyShare, String>
    /** Receive and verify another guardian's SecretKeyShare for me. */
    fun receiveSecretKeyShare(share: SecretKeyShare): Result<SecretKeyShare, String>
}