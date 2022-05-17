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
private val validate = true // expensive, debugging only

/** A Trustee that knows its secret key, for the purpose of decryption. */
data class DecryptingTrustee(
    val id: String,
    val xCoordinate: UInt,
    // this guardian's private and public key
    val electionKeypair: ElGamalKeypair,
    // for all guardians, keyed by guardian id
    val secretKeyShares: Map<String, SecretKeyShare>,
    // for all guardians, keyed by guardian id, the K_ij = g^a_ij
    val coefficientCommitments: Map<String, List<ElementModP>>,
) : DecryptingTrusteeIF {

    override fun id(): String = id
    override fun xCoordinate(): UInt = xCoordinate
    override fun electionPublicKey(): ElementModP = electionKeypair.publicKey.key

    override fun partialDecrypt(
        group: GroupContext,
        texts: List<ElGamalCiphertext>,
        extendedBaseHash: ElementModQ,
        nonce: ElementModQ?
    ): List<DirectDecryptionAndProof> {
        val results: MutableList<DirectDecryptionAndProof> = mutableListOf()
        for (ciphertext: ElGamalCiphertext in texts) {
            val publicKey = this.electionKeypair.publicKey.key
            val privateKey = this.electionKeypair.secretKey.key

            // 𝑀_i = 𝐴^𝑠𝑖 mod 𝑝 (spec section 3.5 eq 9)
            val partialDecryption: ElementModP = ciphertext.computeShare(this.electionKeypair.secretKey)
            // prove that we know x
            val proof: GenericChaumPedersenProof = genericChaumPedersenProofOf(
                g = group.G_MOD_P,
                h = ciphertext.pad,
                x = privateKey,
                nonce ?: group.randomElementModQ(),
                // header and footer doesnt matter as long as it matches isValid
                arrayOf(extendedBaseHash, publicKey, ciphertext.pad, ciphertext.data), // section 7
                arrayOf(partialDecryption),
            )

            if (validate) {
                require(publicKey.equals(group.G_MOD_P powP privateKey))
                require(partialDecryption.equals(ciphertext.pad powP privateKey))
                validate(group, ciphertext, extendedBaseHash, publicKey, partialDecryption, proof)
            }
            results.add(DirectDecryptionAndProof(partialDecryption, proof))
        }
        return results
    }

    override fun compensatedDecrypt(
        group: GroupContext,
        missingGuardianId: String,
        texts: List<ElGamalCiphertext>,
        extendedBaseHash: ElementModQ,
        nonce: ElementModQ?,
    ): List<CompensatedDecryptionAndProof> {
        val backup: SecretKeyShare? = this.secretKeyShares[missingGuardianId]
        if (backup == null) {
            throw IllegalStateException("compensate_decrypt guardian $id missing backup for $missingGuardianId")
        }

        val results: MutableList<CompensatedDecryptionAndProof> = mutableListOf()
        for (ciphertext: ElGamalCiphertext in texts) {
            // used in the calculation, needs to be encrypted
            // 𝑀_{𝑖,l} = 𝐴^P𝑖_{l}
            val partialDecryptionShare: ElementModP =
                ciphertext.computeShare(ElGamalSecretKey(backup.generatingGuardianValue))
            val recoveredPublicKeyShare: ElementModP = recoveredPublicKeyShare(group, missingGuardianId)

            // prove that we know x
            val proof: GenericChaumPedersenProof = genericChaumPedersenProofOf(
                g = group.G_MOD_P,
                h = ciphertext.pad,
                x = backup.generatingGuardianValue,
                seed = nonce ?: group.randomElementModQ(),
                hashHeader = arrayOf(
                    extendedBaseHash,
                    recoveredPublicKeyShare,
                    ciphertext.pad,
                    ciphertext.data
                ), // section 7
                hashFooter = arrayOf(partialDecryptionShare),
            )

            if (validate) {
                require(recoveredPublicKeyShare.equals(group.G_MOD_P powP backup.generatingGuardianValue))
                require(partialDecryptionShare.equals(ciphertext.pad powP backup.generatingGuardianValue))
                validate(group, ciphertext, extendedBaseHash, recoveredPublicKeyShare, partialDecryptionShare, proof)
            }

            results.add(CompensatedDecryptionAndProof(partialDecryptionShare, proof, recoveredPublicKeyShare))
        }
        return results
    }

    private fun validate(
        group: GroupContext,
        ciphertext: ElGamalCiphertext,
        extendedBaseHash: ElementModQ,
        gx: ElementModP,
        hx: ElementModP,
        proof: GenericChaumPedersenProof
    ) {
        if (!proof.isValid(
                group.G_MOD_P,
                gx,
                ciphertext.pad,
                hx,
                arrayOf(extendedBaseHash, gx, ciphertext.pad, ciphertext.data), // section 7
                arrayOf(hx)
            )
        ) {
            logger.warn {
                " partialDecrypt invalid proof for $id = $proof\n" +
                        "   message = $ciphertext\n" +
                        "   gx = $gx\n" +
                        "   hx = $hx\n" +
                        "   extendedBaseHash = $extendedBaseHash\n"
            }
            throw IllegalArgumentException("PartialDecrypt invalid proof for $id")
        }
    }

    // compute the recovered public key share, g^P_i(l) = Prod(K_ij^(l^j)) for j in 0..k-1.
    // see section 3.5.2, eq 12
    private fun recoveredPublicKeyShare(group: GroupContext, missingGuardianId: String): ElementModP {
        val otherCommitments: List<ElementModP>? = this.coefficientCommitments[missingGuardianId] // K_ij
        if (otherCommitments == null) {
            throw IllegalStateException("guardian $id missing coefficientCommitments for $missingGuardianId")
        }
        val xcoordQ: ElementModQ = group.uIntToElementModQ(this.xCoordinate) // = l

        var exponent = group.ONE_MOD_Q
        var result: ElementModP = group.ONE_MOD_P
        for (commitment in otherCommitments) {
            val term = commitment powP exponent
            result = result * term
            exponent = xcoordQ * exponent // = l^j
        }
        return result
    }
}