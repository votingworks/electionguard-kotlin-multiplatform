package electionguard.decrypt

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.randomElementModQ

interface DecryptingTrusteeIF {
    /** Guardian id.  */
    fun id(): String

    /** Guardian x coordinate, for compensated partial decryption  */
    fun xCoordinate(): UInt

    /** The guardian's public key = K_i.  */
    fun electionPublicKey(): ElementModP

    /**
     * Compute a partial decryption of an elgamal encryption.
     *
     * @param texts:            list of `ElGamalCiphertext` that will be partially decrypted
     * @param qbar:             the extended base hash of the election that
     * @param nonceSeed:         an optional value used to generate the `ChaumPedersenProof`
     *                           if no value is provided, a random number will be used.
     * @return a PartialDecryptionProof of the partial decryption and its proof
     */
    fun partialDecrypt(
        group: GroupContext,
        texts : List<ElGamalCiphertext>,
        qbar : ElementModQ,
        nonceSeed: ElementModQ?,
    ): List<PartialDecryptionAndProof>

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
        group: GroupContext,
        missingGuardianId : String,
        texts : List<ElGamalCiphertext>,
        extendedBaseHash : ElementModQ,
        nonceSeed: ElementModQ?,
    ):  List<CompensatedPartialDecryptionAndProof>
}