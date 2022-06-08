package electionguard.decrypt

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext

/** The interface to a DecryptingTrustee. */
interface DecryptingTrusteeIF {
    /** Guardian id.  */
    fun id(): String

    /** Guardian x coordinate, for compensated partial decryption  */
    fun xCoordinate(): Int

    /** The guardian's public key = K_i.  */
    fun electionPublicKey(): ElementModP

    /**
     * Compute a direct partial decryption of an elgamal encryption.
     *
     * @param texts:            list of `ElGamalCiphertext` that will be partially decrypted
     * @param extendedBaseHash: the extended base hash of the election
     * @param nonce:            an optional nonce to generate the `ChaumPedersenProof`
     * @return a list of DirectDecryptionAndProof, in the same order as the texts
     */
    fun directDecrypt(
        group: GroupContext,
        texts: List<ElGamalCiphertext>,
        extendedBaseHash: ElementModQ,
        nonce: ElementModQ?,
    ): List<DirectDecryptionAndProof>

    /**
     * Compute a compensated partial decryption of an elgamal encryption on behalf of the missing guardian.
     *
     * @param missingGuardianId: the missing guardian
     * @param texts:             the ciphertext(s) that will be decrypted
     * @param extendedBaseHash:  the extended base hash of the election
     * @param nonce:             an optional nonce to generate the `ChaumPedersenProof`
     * @return a list of CompensatedDecryptionAndProof, in the same order as the texts
     */
    fun compensatedDecrypt(
        group: GroupContext,
        missingGuardianId: String,
        texts: List<ElGamalCiphertext>,
        extendedBaseHash: ElementModQ,
        nonce: ElementModQ?,
    ): List<CompensatedDecryptionAndProof>
}