package electionguard.decrypt

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElGamalKeypair
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GenericChaumPedersenProof
import electionguard.core.GroupContext
import electionguard.core.genericChaumPedersenProofOf
import electionguard.core.randomElementModQ

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
     * @param extended_base_hash: the extended base hash of the election that
     * @param nonce_seed:         an optional value used to generate the `ChaumPedersenProof`
     *                            if no value is provided, a random number will be used.
     * @return a PartialDecryptionProof of the partial decryption and its proof
     */
    override fun partialDecrypt(
        group : GroupContext,
        texts : List<ElGamalCiphertext>,
        cryptoExtendedBaseHash : ElementModQ,
        nonceSeed: ElementModQ?
    ): List<PartialDecryptionProof> {
        val results: MutableList<PartialDecryptionProof> = ArrayList()
        for (ciphertext: ElGamalCiphertext in texts) {
            // 𝑀_i = 𝐴^𝑠𝑖 mod 𝑝 (spec section 3.5 eq 9)
            val partialDecryption = // ciphertext.decrypt(this.electionKeypair)
                ciphertext.pad powP this.electionKeypair.secretKey.key

            // 𝑀_i = 𝐴^𝑠𝑖 mod 𝑝 and 𝐾𝑖 = 𝑔^𝑠𝑖 mod 𝑝
            //    g: ElementModP,  // G ?
            //    h: ElementModP,  // A = G^r
            //    x: ElementModQ,  // secret key s
            //    seed: ElementModQ,
            //    hashHeader: ElementModQ,
            //    alsoHash: Array<Element> = emptyArray()
            val proof: GenericChaumPedersenProof = genericChaumPedersenProofOf(
                group.G_MOD_P,
                ciphertext.pad, // ??
                this.electionKeypair.secretKey.key, // ??
                nonceSeed?: group.randomElementModQ(), // ok
                cryptoExtendedBaseHash, // ok
            )
            results.add(PartialDecryptionProof(partialDecryption, proof))
        }
        return results
    }


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
    override fun compensatedDecrypt(
        missing_guardian_id : String,
        texts : List<ElGamalCiphertext>,
        extended_base_hash : ElementModQ,
        nonce_seed: ElementModQ?
    ):  List<DecryptionProofRecovery> {
        return emptyList()
    }
}