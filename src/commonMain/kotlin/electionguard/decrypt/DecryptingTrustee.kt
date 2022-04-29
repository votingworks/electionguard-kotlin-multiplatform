package electionguard.decrypt

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElGamalKeypair
import electionguard.core.ElGamalSecretKey
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GenericChaumPedersenProof
import electionguard.core.GroupContext
import electionguard.core.computeShare
import electionguard.core.genericChaumPedersenProofOf
import electionguard.core.isValid
import electionguard.core.randomElementModQ
import electionguard.keyceremony.SecretKeyShare
import mu.KotlinLogging

private val logger = KotlinLogging.logger("DecryptingTrustee")
private val validate = false

/** A Trustee that knows its secret key, for the purpose of decryption. */
data class DecryptingTrustee(
    val id: String,
    val xCoordinate: UInt,
    // this guardian's private and public key
    val electionKeypair: ElGamalKeypair,
    // for all guardians, keyed by guardian id
    val secretKeyShares: Map<String, SecretKeyShare>,
    // for all guardians, keyed by guardian id
    val coefficientCommitments: Map<String, List<ElementModP>>,
) : DecryptingTrusteeIF {

    override fun id(): String = id
    override fun xCoordinate(): UInt = xCoordinate
    override fun electionPublicKey(): ElementModP = electionKeypair.publicKey.key

    /**
     * Compute a partial decryption of an elgamal encryption.
     *
     * @param texts:            list of `ElGamalCiphertext` that will be partially decrypted
     * @param qbar:             the extended base hash of the election
     * @param nonceSeed:        an optional value used to generate the `ChaumPedersenProof`
     *                          if no value is provided, a random number will be used.
     * @return for each text, a partial decryption, and its proof
     */
    override fun partialDecrypt(
        group: GroupContext,
        texts: List<ElGamalCiphertext>,
        qbar: ElementModQ,
        nonceSeed: ElementModQ?
    ): List<PartialDecryptionAndProof> {
        val results: MutableList<PartialDecryptionAndProof> = mutableListOf()
        for (ciphertext: ElGamalCiphertext in texts) {
            // 𝑀_i = 𝐴^𝑠𝑖 mod 𝑝 (spec section 3.5 eq 9)
            val partialDecryption: ElementModP = ciphertext.computeShare(this.electionKeypair.secretKey)
            val publicKey = this.electionKeypair.publicKey.key

            val proof: GenericChaumPedersenProof = genericChaumPedersenProofOf(
                group.G_MOD_P,
                ciphertext.pad,
                this.electionKeypair.secretKey.key,
                nonceSeed ?: group.randomElementModQ(),
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
                )
            ) {
                logger.warn {
                    " partialDecrypt invalid proof for $id = $proof\n" +
                            "   message = $ciphertext\n" +
                            "   public_key = $publicKey\n" +
                            "   partial_decryption = $partialDecryption\n" +
                            "   qbar = $qbar\n"
                }

                throw IllegalArgumentException("PartialDecrypt invalid proof for $id")
            }

            results.add(PartialDecryptionAndProof(partialDecryption, proof))
        }
        return results
    }

    override fun compensatedDecrypt(
        group: GroupContext,
        missingGuardianId: String,
        texts: List<ElGamalCiphertext>,
        extendedBaseHash: ElementModQ,
        nonceSeed: ElementModQ?,
    ): List<CompensatedPartialDecryptionAndProof> {

        val backup: SecretKeyShare? = this.secretKeyShares[missingGuardianId]
        if (backup == null) {
            throw IllegalStateException("compensate_decrypt guardian $id missing backup for $missingGuardianId")
        }
        val recoveredPublicKey: ElementModP = recoverPublicKey(group, missingGuardianId)
        val results: MutableList<CompensatedPartialDecryptionAndProof> = mutableListOf()
        for (ciphertext: ElGamalCiphertext in texts) {
            // 𝑀_{𝑖,l} = 𝐴^P𝑖_{l}
            val partialDecryption: ElementModP =
                ciphertext.computeShare(ElGamalSecretKey(backup.generatingGuardianValue))
            results.add(CompensatedPartialDecryptionAndProof(partialDecryption, null, recoveredPublicKey))
        }
        return results
    }

    fun recoverPublicKey(group: GroupContext, missingGuardianId: String): ElementModP {
        val otherCommitments: List<ElementModP>? = this.coefficientCommitments[missingGuardianId]
        if (otherCommitments == null) {
            throw IllegalStateException("guardian $id missing coefficientCommitments for $missingGuardianId")
        }
        val xcoordQ: ElementModQ = group.uIntToElementModQ(this.xCoordinate)

        // compute the recovered public key, corresponding to the secret share Pi(l) = K_ij^(l^j) for j in 0..k-1.
        var exponent = group.ONE_MOD_Q
        var result: ElementModP = group.ONE_MOD_P
        for (commitment in otherCommitments) {
            val term = commitment powP exponent
            result = result * term
            exponent = xcoordQ * exponent
        }
        return result
    }
}