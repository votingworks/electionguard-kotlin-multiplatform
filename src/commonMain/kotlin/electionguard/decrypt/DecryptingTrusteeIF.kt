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
     * @param extended_base_hash: the extended base hash of the election that
     * @param nonce_seed:         an optional value used to generate the `ChaumPedersenProof`
     *                            if no value is provided, a random number will be used.
     * @return a PartialDecryptionProof of the partial decryption and its proof
     */
    fun partialDecrypt(
        group: GroupContext,
        texts : List<ElGamalCiphertext>,
        extended_base_hash : ElementModQ,
        nonce_seed: ElementModQ?
    ): List<PartialDecryptionProof>


    /**
     * Compute a compensated partial decryption of an elgamal encryption on behalf of the missing guardian.
     *
     * @param missing_guardian_id: the guardian
     * @param texts:               the ciphertext(s) that will be decrypted
     * @param extended_base_hash:  the extended base hash of the election used to generate the ElGamal Ciphertext
     * @param nonce_seed:          an optional value used to generate the `ChaumPedersenProof`
     *                             if no value is provided, a random number will be used.
     * @return a DecryptionProofRecovery with the decryption and its proof and a recovery key
     */
    fun compensatedDecrypt(
        missing_guardian_id : String,
        texts : List<ElGamalCiphertext>,
        extended_base_hash : ElementModQ,
        nonce_seed: ElementModQ?
    ):  List<DecryptionProofRecovery>
}