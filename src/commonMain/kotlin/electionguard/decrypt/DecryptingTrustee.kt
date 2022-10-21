package electionguard.decrypt

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElGamalKeypair
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.computeShare
import electionguard.core.decrypt
import electionguard.core.randomElementModQ
import electionguard.core.toElementModQ
import electionguard.core.toUInt256
import electionguard.keyceremony.SecretKeyShare

/**
 * A Trustee that knows its own secret key, for the purpose of decryption.
 * DecryptingTrustee must stay private. DecryptingGuardian is its public info in the election record.
 */
data class DecryptingTrustee(
    val id: String,
    val xCoordinate: Int,
    // This guardian's private and public key
    val electionKeypair: ElGamalKeypair,
    // Other guardians' shares of this guardian's secret key, keyed by generating guardian id.
    val secretKeyShares: Map<String, SecretKeyShare>,
) : DecryptingTrusteeIF {
    // these will be constructed lazily as needed. keyed by missing_id = generating guardian
    // Pj(ℓ) = value of other's secret polynomial at my coordinate = "my share of other's secret key"
    private val generatingGuardianValues = mutableMapOf<String, ElementModQ>()

    init {
        require(xCoordinate > 0)
    }

    override fun id(): String = id
    override fun xCoordinate(): Int = xCoordinate
    override fun electionPublicKey(): ElementModP = electionKeypair.publicKey.key

    override fun decrypt(
        group: GroupContext,
        lagrangeCoeff: ElementModQ,
        missingGuardians: List<String>,
        texts: List<ElGamalCiphertext>,
        nonce: ElementModQ?
    ): List<PartialDecryption> {
        val results: MutableList<PartialDecryption> = mutableListOf()
        for (ciphertext: ElGamalCiphertext in texts) {
            val privateKey = this.electionKeypair.secretKey.key

            // ti = (si + wi * Sum(Pj(i))j∈V) (spec 1.52, eg 58)
            val ti = if (missingGuardians.isEmpty()) privateKey else {
                val pjls = missingGuardians.map { computeShare(group, it) }
                val sumPjls = with(group) { pjls.addQ() }
                privateKey + lagrangeCoeff * sumPjls // ti = (si + wi * (Sum j∈V Pj (i))) mod q
            }
            val u = nonce?: group.randomElementModQ(2)
            val a = group.gPowP(u)
            val b = ciphertext.pad powP u
            val Mbari = ciphertext.computeShare(ti) // Mbar_i = A ^ t_i
            results.add(PartialDecryption(id, Mbari, u, a, b, ti - privateKey))
        }
        return results
    }

    // lazy decryption of key share.
    // encrypted: El(Pj(ℓ)) = spec 1.52, section 3.2.2 eq 14
    // decrypted = Pj(ℓ)
    private fun computeShare(group: GroupContext, missingGuardianId : String) : ElementModQ {
        var generatingGuardianValue = this.generatingGuardianValues[missingGuardianId]
        if (generatingGuardianValue == null) {
            val secretKeyShare: SecretKeyShare = this.secretKeyShares[missingGuardianId]
                ?: throw IllegalStateException("compensate_decrypt guardian $id missing SecretKeyShare for $missingGuardianId")
            val byteArray = secretKeyShare.encryptedCoordinate.decrypt(this.electionKeypair.secretKey)
                ?: throw IllegalStateException("$id SecretKeyShare for $id couldnt decrypt SecretKeyShare for $missingGuardianId ")
            generatingGuardianValue = byteArray.toUInt256().toElementModQ(group)
            this.generatingGuardianValues[missingGuardianId] = generatingGuardianValue
        }
        return generatingGuardianValue
    }

    override fun challenge(
        group: GroupContext,
        challenges: List<ChallengeRequest>,
    ): List<ChallengeResponse> {
        return challenges.map {
            ChallengeResponse(it.id, it.nonce - it.challenge * ( electionKeypair.secretKey.key + it.tm))
        }
    }
}