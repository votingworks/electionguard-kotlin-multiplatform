package electionguard.ballot

import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.core.SchnorrProof
import electionguard.keyceremony.calculateGexpPiAtL

/** Public info for the ith Guardian/Trustee. */
data class Guardian(
    val guardianId: String,
    val xCoordinate: Int, // use sequential numbering starting at 1; = i of T_i, K_i
    val coefficientProofs: List<SchnorrProof> // the order is the same as coordinates
) {
    init {
        require(guardianId.isNotEmpty())
        require(xCoordinate > 0)
        require(coefficientProofs.isNotEmpty())
    }
    fun publicKey() : ElementModP = coefficientProofs[0].publicKey

    // the K_ij, where K_i0 = Ki
    fun coefficientCommitments(): List<ElementModP> {
        return coefficientProofs.map { it.publicKey }
    }
}