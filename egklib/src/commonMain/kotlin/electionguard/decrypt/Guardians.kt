package electionguard.decrypt

import electionguard.ballot.Guardian
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.keyceremony.calculateGexpPiAtL

/** All the guardians, not just the decrypting ones. */
data class Guardians(val group : GroupContext, val guardians: List<Guardian>) {
    val guardianMap = guardians.associateBy { it.guardianId }
    val guardianGexpP = mutableMapOf<String, ElementModP>()

    /**
     * g^P(ℓ) mod p = Prod_i( g^Pi(ℓ) ), i = 1..n
     *
     * g raised to P(xcoord), where P = P1(xcoord) + P2(xcoord) + .. + Pn(xcoord)
     * and xcoord is the xcoordinate of guardianId, aka ℓ in eq 21.
     * This is the inner factor in eqs 74, 83.
     */
    fun getGexpP(guardianId: String) : ElementModP {
        return guardianGexpP.getOrPut(guardianId) {
            val guardian = guardianMap[guardianId]
                ?: throw IllegalStateException("Guardians.getGexpP doesnt have guardian id = '$guardianId'")

            with(group) {
                guardians.map { calculateGexpPiAtL(it.guardianId, guardian.xCoordinate, it.coefficientCommitments()) }.multP()
            }
        }
    }

}