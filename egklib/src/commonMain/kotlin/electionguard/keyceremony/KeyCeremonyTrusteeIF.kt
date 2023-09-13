package electionguard.keyceremony

import com.github.michaelbull.result.Result
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
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

    /** Create another guardians share of my key, encrypted. */
    fun encryptedKeyShareFor(otherGuardian: String): Result<EncryptedKeyShare, String>
    /** Receive and verify an encrypted key share. */
    fun receiveEncryptedKeyShare(share: EncryptedKeyShare?): Result<Boolean, String>

    /** Create another guardians share of my key, not encrypted. */
    fun keyShareFor(otherGuardian: String): Result<KeyShare, String>
    /** Receive and verify a key share. */
    fun receiveKeyShare(keyShare: KeyShare): Result<Boolean, String>

    /** call after all shares are added, and before calling secretKeyShare() */
    fun computeSecretKeyShare(nguardians : Int): Result<ElementModQ, String>

    /** The resulting secretKeyShare for this guardian == (P1(ℓ) + P2(ℓ) + · · · + Pn(ℓ)) mod q. spec 2.0.0, eq 65. */
    fun secretKeyShare(): ElementModQ
}