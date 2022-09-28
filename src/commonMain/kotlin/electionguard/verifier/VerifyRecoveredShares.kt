package electionguard.verifier

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import electionguard.ballot.DecryptingGuardian
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.getSystemTimeInMillis
import electionguard.decrypt.PartialDecryption
import electionguard.decrypt.computeLagrangeCoefficient
import electionguard.publish.ElectionRecord

// TODO check this with 1.51 Verification box 10
/** When there are missing guardians, check "replacement partial decryptions" (box 10). */
class VerifyRecoveredShares(
    val group: GroupContext,
    val record: ElectionRecord
) {
    val decryptingGuardians : List<DecryptingGuardian>
    val lagrangeCoefficients: Map<String, ElementModQ>
    val decryptedTallyOrBallot : DecryptedTallyOrBallot

    init {
        decryptingGuardians = record.decryptingGuardians()
        lagrangeCoefficients = decryptingGuardians.associate { it.guardianId to it.lagrangeCoordinate }
        decryptedTallyOrBallot = record.decryptedTally()!!
    }

    fun verify(showTime : Boolean = false): Result<Boolean, String> {
        val starting = getSystemTimeInMillis()
        val decryptingGuardianCount = decryptingGuardians.size
        if (decryptingGuardianCount == record.numberOfGuardians()) {
            println(" Does not have missing guardians")
            return Ok(true)
        }
        val errors = getAllErrors(verifyLagrangeCoefficients(), verifyShares())
        val took = getSystemTimeInMillis() - starting
        if (showTime) println("   VerifyRecoveredShares took $took millisecs")
        return if (errors.isEmpty()) Ok(true) else Err(errors.joinToString("\n"))
    }

    /** Verify 10.A for available guardians lagrange coefficients, if there are missing guardians.  */
    fun verifyLagrangeCoefficients(): Result<Boolean, String> {
        val errors = mutableListOf<String>()
        for (guardian in decryptingGuardians) {
            val seqOthers = mutableListOf<Int>()
            for (other in decryptingGuardians) {
                if (!other.guardianId.equals(guardian.guardianId)) {
                    seqOthers.add(other.xCoordinate)
                }
            }
            if (!verifyLagrangeCoefficient(guardian.xCoordinate, seqOthers, guardian.lagrangeCoordinate)) {
                errors.add(" *** 10.A Lagrange coefficients failure for guardian ${guardian.guardianId}")
            }
        }
        return if (errors.isEmpty()) Ok(true) else Err(errors.joinToString("\n"))
    }

    // 10.A. An election verifier should confirm that for each trustee T_l serving to help compute a missing
    // share of a tally, that its Lagrange coefficient w_l is correctly computed by confirming the equation
    //     (∏ j∈(U−{l}) j) mod q = (w_l ⋅ (∏ j∈(U−{l}) (j − l) )) mod q
    fun verifyLagrangeCoefficient(coordinate: Int, present: List<Int>, expected: ElementModQ): Boolean {
        val computed = group.computeLagrangeCoefficient(coordinate, present)
        return expected == computed
    }

    // 10.B Confirm missing tally shares
    private fun verifyShares(): Result<Boolean, String> {
        val errors = mutableListOf<String>()
        for (contest in decryptedTallyOrBallot.contests.values) {
            for (selection in contest.selections.values) {
                val id: String = contest.contestId + "-" + selection.selectionId
                for (partial in selection.partialDecryptions) {
                    if (partial.recoveredDecryptions.isNotEmpty()) {
                        if (!verify10B(partial)) {
                            errors.add(" *** 10.B verify replacement share $id for Guardian ${partial.guardianId} failed")
                        }
                    }
                }
            }
        }
        return if (errors.isEmpty()) Ok(true) else Err(errors.joinToString("\n"))
    }

    // 10.B Confirm the correct missing tally share for each (non-placeholder) option
    // in each contest in the ballot coding file for each missing trustee T_i as
    //    M_i = ∏ l∈U (M_i,l)^w_l mod p
    // guardian i is missing, l is available
    private fun verify10B(partial: PartialDecryption): Boolean {
        var product: ElementModP = group.ONE_MOD_P
        for (compShare in partial.recoveredDecryptions) {
            val M_il: ElementModP = compShare.share // M_i,l in the spec
            val lagrange: ElementModQ = lagrangeCoefficients[compShare.decryptingGuardianId]
                ?: throw IllegalStateException("Cant find lagrange coefficient for " + compShare.decryptingGuardianId)
            val term = M_il powP lagrange
            product = product * term
        }

        val M_i: ElementModP = partial.share()
        return M_i.equals(product)
    }
}