package electionguard.decrypt

import com.github.michaelbull.result.Err
import electionguard.core.ElGamalCiphertext
import electionguard.core.ElGamalKeypair
import electionguard.core.ElGamalSecretKey
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GenericChaumPedersenProof
import electionguard.core.GroupContext
import electionguard.core.computeShare
import electionguard.core.decrypt
import electionguard.core.genericChaumPedersenProofOf
import electionguard.core.isValid
import electionguard.core.randomElementModQ
import electionguard.core.toElementModQ
import electionguard.core.toUInt256
import electionguard.keyceremony.SecretKeyShare
import electionguard.keyceremony.calculateGexpPiAtL
import mu.KotlinLogging

private val logger = KotlinLogging.logger("DecryptingTrustee")
private const val validate = true // expensive, debugging only

/**
 * A Trustee that knows its own secret key, for the purpose of decryption.
 * DecryptingTrustee must stay private. DecryptingGuardian is its public info in the election record.
 */
data class DecryptingTrustee(
    val id: String,
    val xCoordinate: Int,
    // This guardian's private and public key
    val electionKeypair: ElGamalKeypair,
    // Other guardians' shares of this guardian's secret key, keyed by generating (missing) guardian id.
    val secretKeyShares: Map<String, SecretKeyShare>,
    // Other guardians' coefficient commitments, K_ij = g^a_ij, keyed by guardian id.
    val guardianPublicKeys: Map<String, List<ElementModP>>,
) : DecryptingTrusteeIF {
    // these will be constructed lazily if needed. keyed by missing_id
    private val generatingGuardianValues = mutableMapOf<String, ElementModQ>()
    private val gPilMap = mutableMapOf<String, ElementModP>()

    init {
        require(xCoordinate > 0)
    }

    override fun id(): String = id
    override fun xCoordinate(): Int = xCoordinate
    override fun electionPublicKey(): ElementModP = electionKeypair.publicKey.key

    override fun directDecrypt(
        group: GroupContext,
        texts: List<ElGamalCiphertext>,
        extendedBaseHash: ElementModQ,
        nonce: ElementModQ?
    ): List<DirectDecryptionAndProof> {
        val results: MutableList<DirectDecryptionAndProof> = mutableListOf()
        for (ciphertext: ElGamalCiphertext in texts) {
            val publicKey = this.electionKeypair.publicKey.key
            val privateKey = this.electionKeypair.secretKey.key

            // 𝑀_i = 𝐴^𝑠𝑖 mod 𝑝 (spec 1.51 section 3.5 eq 64)
            val partialDecryption: ElementModP = ciphertext.computeShare(this.electionKeypair.secretKey)
            // prove that we know x
            val proof: GenericChaumPedersenProof = genericChaumPedersenProofOf(
                g = group.G_MOD_P,
                h = ciphertext.pad,
                x = privateKey,
                nonce ?: group.randomElementModQ(),
                // header and footer doesnt matter as long as it matches isValid()
                // other than spec compliance
                arrayOf(extendedBaseHash, publicKey, ciphertext.pad, ciphertext.data), // section 7
                arrayOf(partialDecryption),
            )

            if (validate) {
                require(publicKey == group.G_MOD_P powP privateKey)
                require(partialDecryption == ciphertext.pad powP privateKey)
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

        // lazy decryption of key share.
        var generatingGuardianValue = this.generatingGuardianValues[missingGuardianId]
        if (generatingGuardianValue == null) {
            val backup: SecretKeyShare = this.secretKeyShares[missingGuardianId]
                ?: throw IllegalStateException("compensate_decrypt guardian $id missing backup for $missingGuardianId")
            val byteArray = backup.encryptedCoordinate.decrypt(this.electionKeypair.secretKey)
                ?: throw IllegalStateException("$id backup for $missingGuardianId couldnt decrypt encryptedCoordinate")
            generatingGuardianValue = byteArray.toUInt256().toElementModQ(group)
            this.generatingGuardianValues[missingGuardianId] = generatingGuardianValue
        }

        // lazy calculation of g^Pi(l).
        var gPil = this.gPilMap[missingGuardianId]
        if (gPil == null) {
            val coefficientCommitments: List<ElementModP> = this.guardianPublicKeys[missingGuardianId]
                ?: throw IllegalStateException("guardian $id missing coefficientCommitments for $missingGuardianId")
            gPil = calculateGexpPiAtL(this.xCoordinate, coefficientCommitments)
            this.gPilMap[missingGuardianId] = gPil
        }

        val results: MutableList<CompensatedDecryptionAndProof> = mutableListOf()
        for (ciphertext: ElGamalCiphertext in texts) {
            // 𝑀_{𝑖,l} = 𝐴^P𝑖_{l} (spec 1.51 section 3.5.2 eq 59)
            val partialDecryptionShare: ElementModP =
                ciphertext.computeShare(ElGamalSecretKey(generatingGuardianValue))

            // prove that we know x
            val proof: GenericChaumPedersenProof = genericChaumPedersenProofOf(
                g = group.G_MOD_P,
                h = ciphertext.pad,
                x = generatingGuardianValue,
                seed = nonce ?: group.randomElementModQ(),
                hashHeader = arrayOf(
                    extendedBaseHash,
                    gPil,
                    ciphertext.pad,
                    ciphertext.data
                ), // section 7
                hashFooter = arrayOf(partialDecryptionShare),
            )

            if (validate) {
                require(gPil == (group.G_MOD_P powP generatingGuardianValue))
                require(partialDecryptionShare == (ciphertext.pad powP generatingGuardianValue))
                validate(group, ciphertext, extendedBaseHash, gPil, partialDecryptionShare, proof)
            }

            results.add(CompensatedDecryptionAndProof(partialDecryptionShare, proof, gPil))
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
        val proofResult = proof.isValid(
                group.G_MOD_P,
                gx,
                ciphertext.pad,
                hx,
                arrayOf(extendedBaseHash, gx, ciphertext.pad, ciphertext.data), // section 7
                arrayOf(hx)
            )
        if (proofResult is Err) {
            logger.warn { " partialDecrypt invalid proof for $id = $proofResult.error" }
            throw IllegalArgumentException("PartialDecrypt invalid proof for $id")
        }
    }
}