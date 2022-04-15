package electionguard.decrypt

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElGamalKeypair
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GenericChaumPedersenProof
import electionguard.core.GroupContext
import electionguard.core.computeShare
import electionguard.core.genericChaumPedersenProofOf
import electionguard.core.isValid
import electionguard.core.randomElementModQ
import mu.KotlinLogging

private val logger = KotlinLogging.logger("DecryptingTrustee")
private val validate = false

class DecryptingTrustee(val id : String, val xCoordinate: Int, val electionKeypair: ElGamalKeypair)
    : DecryptingTrusteeIF {
    /** Guardian id.  */
    override fun id(): String = id

    /** Guardian x coordinate.  */
    override fun xCoordinate(): Int = xCoordinate

    /** Elgamal election public key = K_i.  */
    override fun electionPublicKey(): ElementModP = electionKeypair.publicKey.key

    /**
     * Compute a partial decryption of an elgamal encryption.
     *
     * @param texts:            list of `ElGamalCiphertext` that will be partially decrypted
     * @param qbar: the extended base hash of the election
     * @param nonceSeed:         an optional value used to generate the `ChaumPedersenProof`
     *                            if no value is provided, a random number will be used.
     * @return a PartialDecryptionProof of the partial decryption and its proof
     */
    override fun partialDecrypt(
        group : GroupContext,
        texts : List<ElGamalCiphertext>,
        qbar : ElementModQ,
        nonceSeed: ElementModQ?
    ): List<PartialDecryptionProof> {
        val results: MutableList<PartialDecryptionProof> = ArrayList()
        for (ciphertext: ElGamalCiphertext in texts) {
            // 𝑀_i = 𝐴^𝑠𝑖 mod 𝑝 (spec section 3.5 eq 9)
            val partialDecryption: ElementModP = ciphertext.computeShare(this.electionKeypair.secretKey)
            val publicKey = this.electionKeypair.publicKey.key

            val proof: GenericChaumPedersenProof = genericChaumPedersenProofOf(
                group.G_MOD_P,
                ciphertext.pad,
                this.electionKeypair.secretKey.key,
                nonceSeed?: group.randomElementModQ(),
                arrayOf(qbar, publicKey, ciphertext.pad, ciphertext.data), // section 7
                arrayOf(partialDecryption),
            )

            if (validate && !proof.isValid(
                    group.G_MOD_P,
                    publicKey,
                    ciphertext.pad,
                    partialDecryption,
                    arrayOf(qbar, publicKey, ciphertext.pad, ciphertext.data), // section 7
                    arrayOf(partialDecryption)
                )) {
                logger.warn {
                    " partialDecrypt invalid proof for $id = $proof\n" +
                            "   message = $ciphertext\n" +
                            "   public_key = $publicKey\n" +
                            "   partial_decryption = $partialDecryption\n" +
                            "   qbar = $qbar\n" }

                throw IllegalArgumentException("PartialDecrypt invalid proof for $id")
            }

            results.add(PartialDecryptionProof(partialDecryption, proof))
        }
        return results
    }


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
    override fun compensatedDecrypt(
        missingGuardianId : String,
        texts : List<ElGamalCiphertext>,
        extendedBaseHash : ElementModQ,
        nonceSeed: ElementModQ?
    ):  List<DecryptionProofRecovery> {
        return emptyList()
    }
}