package electionguard.decrypt

import electionguard.core.*

/**
 * A Trustee that has a share of the election private key, for the purpose of decryption.
 * DecryptingTrustee must stay private. Guardian is its public info in the election record.
 */
data class DecryptingTrusteeDoerre(
    val id: String,
    val xCoordinate: Int,
    val publicKey: ElementModP, // Must match the public record
    val keyShare: ElementModQ, // P(i) = (P1 (i) + P2 (i) + · · · + Pn (i)) eq 65
    ) : DecryptingTrusteeIF {

    val group = compatibleContextOrFail(publicKey, keyShare)
    // problem with this is now there's state; if trustee goes down it cant come back up and continue, if there are
    // PartialDecryption not yet challenged, since there will be a different randomConstantNonce.
    private val randomConstantNonce = group.randomElementModQ(2) // random value u in Zq

    init {
        require(xCoordinate > 0)
    }

    override fun id(): String = id
    override fun xCoordinate(): Int = xCoordinate
    override fun guardianPublicKey(): ElementModP = publicKey

    override fun decrypt(
        group: GroupContext,
        texts: List<ElementModP>,
    ): List<PartialDecryption> {
        val results: MutableList<PartialDecryption> = mutableListOf()
        for (text: ElementModP in texts) {
            if (!text.isValidResidue()) {
                return emptyList()
            }
            val u = group.randomElementModQ(2) // random value u in Zq
            val a = group.gPowP(u)  // (a,b) for the proof, spec 2.0.0, eq 69
            val b = text powP u
            val mi = text powP keyShare // Mi = A ^ P(i), spec 2.0.0, eq 66
            // TODO controversial to send u, could cache it here.
            // try adding a constant random nonce to ui, and subtract it on the challenge.
            results.add( PartialDecryption(id, mi, u + randomConstantNonce, a, b))
        }
        return results
    }

    override fun challenge(
        group: GroupContext,
        challenges: List<ChallengeRequest>,
    ): List<ChallengeResponse> {
        return challenges.map {
            ChallengeResponse(it.id, it.nonce - randomConstantNonce - it.challenge * keyShare) // spec 2.0.0, eq 73
        }
    }
}