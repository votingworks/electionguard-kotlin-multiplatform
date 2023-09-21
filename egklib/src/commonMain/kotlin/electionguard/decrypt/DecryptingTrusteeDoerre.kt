package electionguard.decrypt

import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.randomElementModQ

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
            val u = group.randomElementModQ(2) // random value u in Zq
            val a = group.gPowP(u)  // (a,b) for the proof, spec 2.0.0, eq 69
            val b = text powP u
            val mi = text powP keyShare // Mi = A ^ P(i), spec 2.0.0, eq 66
            // controversial to send u, could cache it here.
            // could add a constant random nonce to ui, and subtract it on the challenge?
            results.add( PartialDecryption(id, mi, u, a, b))
        }
        return results
    }

    override fun challenge(
        group: GroupContext,
        challenges: List<ChallengeRequest>,
    ): List<ChallengeResponse> {
        return challenges.map {
            ChallengeResponse(it.id, it.nonce - it.challenge * keyShare) // spec 2.0.0, eq 73
        }
    }
}