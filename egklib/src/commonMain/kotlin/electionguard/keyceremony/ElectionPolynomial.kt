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

    /** The public coefficient commitments `K_j = g^a_j`. */
    val coefficientCommitments: List<ElementModP>,

    /** A public proof of possession for each secret coefficient. */
    val coefficientProofs: List<SchnorrProof>,
) {
    init {
        require(guardianId.isNotEmpty())
        require(coefficients.isNotEmpty())
        require(coefficients.size == coefficientCommitments.size)
        require(coefficients.size == coefficientProofs.size)
    }

    /** The value of the polynomial at xcoord. This is private information, only shared in encrypted form. */
    fun valueAt(group: GroupContext, xcoord : Int): ElementModQ {
        val xcoordQ: ElementModQ = group.uIntToElementModQ(xcoord.toUInt())
        var result: ElementModQ = group.ZERO_MOD_Q
        var xcoordPower: ElementModQ = group.ONE_MOD_Q

        for (coefficient in this.coefficients) {
            val term = coefficient * xcoordPower
            result += term
            xcoordPower *= xcoordQ
        }
        return result
    }
}

/**
 * Calculate g^Pi(ℓ) mod p = Product ((K_i,j)^ℓ^j) mod p, j = 0, quorum-1.
 * Used to test secret key share by KeyCeremonyTrustee.
 */
fun calculateGexpPiAtL(
    xcoord: Int,  // l
    coefficientCommitments: List<ElementModP>  // K_i,j
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

/** Generate random coefficients for a polynomial of degree quorum-1. */
fun GroupContext.generatePolynomial(
    guardianId: String,
    quorum: Int,
): ElectionPolynomial {
    val coefficients = mutableListOf<ElementModQ>()
    val commitments = mutableListOf<ElementModP>()
    val proofs = mutableListOf<SchnorrProof>()

    for (coeff in 0 until quorum) {
        val keypair: ElGamalKeypair = elGamalKeyPairFromRandom(this)
        coefficients.add(keypair.secretKey.key)
        commitments.add(keypair.publicKey.key)
        proofs.add(keypair.schnorrProof())
    }
    return ElectionPolynomial(guardianId, coefficients, commitments, proofs)
}