package electionguard.decrypt

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext

interface DecryptingTrusteeIF {
    /** Guardian id.  */
    fun id(): String

    /** Guardian x coordinate.  */
    fun xCoordinate(): Int

    /** Elgamal election public key = K_i.  */
    fun electionPublicKey(): ElementModP

    /**
     * Compute a partial decryption of an elgamal encryption.
     *
     * @param texts:            list of `ElGamalCiphertext` that will be partially decrypted
     * @param extendedBaseHash: the extended base hash of the election that
     * @param nonceSeed:         an optional value used to generate the `ChaumPedersenProof`
     *                            if no value is provided, a random number will be used.
     * @return a PartialDecryptionProof of the partial decryption and its proof
     */
    fun partialDecrypt(
        group: GroupContext,
        texts : List<ElGamalCiphertext>,
        extendedBaseHash : ElementModQ,
        nonceSeed: ElementModQ?
    ): List<PartialDecryptionProof>


    /**
     * Compute a compensated partial decryption of an elgamal encryption on behalf of the missing guardian.
     *
     * @param missingGuardianId: the guardian
     * @param texts:               the ciphertext(s) that will be decrypted
     * @param extendedBaseHash:  the extended base hash of the election used to generate the ElGamal Ciphertext
     * @param nonceSeed:          an optional value used to generate the `ChaumPedersenProof`
     *                             if no value is provided, a random number will be used.
     * @return a DecryptionProofRecovery with the decryption and its proof and a recovery key
     */
    fun compensatedDecrypt(
        missingGuardianId : String,
        texts : List<ElGamalCiphertext>,
        extendedBaseHash : ElementModQ,
        nonceSeed: ElementModQ?
    ):  List<DecryptionProofRecovery>
}