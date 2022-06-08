package electionguard.keyceremony

import electionguard.core.ElGamalKeypair
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.SchnorrProof
import electionguard.core.compatibleContextOrFail
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
    fun valueAt(group: GroupContext, xcoord : Int): ElementModQ {
        val xcoordQ: ElementModQ = group.uIntToElementModQ(xcoord.toUInt())
        var computedValue: ElementModQ = group.ZERO_MOD_Q
        var xcoordPower: ElementModQ = group.ONE_MOD_Q

        for (coefficient in this.coefficients) {
            val term = coefficient * xcoordPower
            computedValue += term
            xcoordPower *= xcoordQ
        }
        return computedValue
    }
}

// Used in KeyCeremonyTrustee and DecryptingTrustee
// g^Pi(ℓ) mod p = Product ((K_i,j)^ℓ^j) mod p, j = 0, k-1 because there are always k coefficients
fun calculateGexpPiAtL(
    xcoord: Int,  // l
    coefficientCommitments: List<ElementModP>  // the committments to Pi
): ElementModP {
    val group = compatibleContextOrFail(*coefficientCommitments.toTypedArray())
    val xcoordQ: ElementModQ = group.uIntToElementModQ(xcoord.toUInt())
    var result: ElementModP = group.ONE_MOD_P
    var xcoordPower: ElementModQ = group.ONE_MOD_Q // ℓ^j

    for (commitment in coefficientCommitments) {
        val term = commitment powP xcoordPower // (K_i,j)^ℓ^j
        result *= term
        xcoordPower *= xcoordQ
    }
    return result
}

fun GroupContext.generatePolynomial(
    id: String,
    quorum: Int,
): ElectionPolynomial {
    val coefficients = mutableListOf<ElementModQ>()
    val commitments = mutableListOf<ElementModP>()
    val proofs = mutableListOf<SchnorrProof>()

    for (coeff in 1..quorum) {
        val keypair: ElGamalKeypair = elGamalKeyPairFromRandom(this)
        coefficients.add(keypair.secretKey.key)
        commitments.add(keypair.publicKey.key)
        proofs.add(keypair.schnorrProof())
    }
    return ElectionPolynomial(id, coefficients, commitments, proofs)
}