package electionguard.keyceremony

import electionguard.core.ElGamalKeypair
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.SchnorrProof
import electionguard.core.elGamalKeyPairFromRandom
import electionguard.core.schnorrProof

data class ElectionPolynomial(
    val guardianId: String,

    /** The secret coefficients `a_j, j = 1..k`  */
    val coefficients: List<ElementModQ>,

    /** The coefficient commitments `K_j = g^a_j`. */
    val coefficientCommitments: List<ElementModP>,

    /** A proof of possession of the private key for each secret coefficient. */
    val coefficientProofs: List<SchnorrProof>,
) {
    // The value of the polynomial at xcoord
    fun valueAt(group: GroupContext, xcoord : UInt): ElementModQ {
        val xcoordQ: ElementModQ = group.uIntToElementModQ(xcoord)
        var computed_value: ElementModQ = group.ZERO_MOD_Q
        var xcoordPower: ElementModQ = group.ONE_MOD_Q

        for (coefficient in this.coefficients) {
            val term = coefficient * xcoordPower
            computed_value = computed_value + term
            xcoordPower = xcoordPower * xcoordQ
        }
        return computed_value
    }
}

fun generatePolynomial(
    id: String,
    context: GroupContext,
    quorum: Int,
): ElectionPolynomial {
    val coefficients = mutableListOf<ElementModQ>()
    val commitments = mutableListOf<ElementModP>()
    val proofs = mutableListOf<SchnorrProof>()

    for (coeff in 1..quorum) {
        val keypair: ElGamalKeypair = elGamalKeyPairFromRandom(context)
        coefficients.add(keypair.secretKey.key)
        commitments.add(keypair.publicKey.key)
        proofs.add(keypair.schnorrProof())
    }
    return ElectionPolynomial(id, coefficients, commitments, proofs)
}