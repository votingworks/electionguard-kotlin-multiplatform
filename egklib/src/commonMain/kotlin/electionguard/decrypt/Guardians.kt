package electionguard.decrypt

import electionguard.ballot.Guardian
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.keyceremony.calculateGexpPiAtL

data class Guardians(val group : GroupContext, val guardians: List<Guardian>) {
    val guardianMap = guardians.associateBy { it.guardianId }
    val guardianGexpP = mutableMapOf<String, ElementModP>()

    /**
     * g raised to Pi(xcoord), where P is the sum of all the trustee's secret polynomials.
     * Can be calculated entirely from the public commitments (K_j,m):
     *    g^P(xcoord) = Prod (Prod ( (K_j,m)^xcoord^m ), m=0,k-1), j=1,n)
     *  This is the innermost factor of eq 74.
     */
    fun getGexpP(guardianId: String) : ElementModP {
        return guardianGexpP.getOrPut(guardianId) {
            val guardian = guardianMap[guardianId]
                ?: throw IllegalStateException("*** calculateGexpP guardian not found for $guardianId")
            with(group) {
                guardians.map { calculateGexpPiAtL(guardian.xCoordinate, it.coefficientCommitments()) }.multP()
            }
        }
    }

}