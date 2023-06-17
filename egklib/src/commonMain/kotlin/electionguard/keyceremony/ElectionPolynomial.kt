package electionguard.keyceremony

import electionguard.core.*

/** Pi(x), spec 1.9, section 3.2.1. Must be kept secret. */
data class ElectionPolynomial(
    val guardianId: String,  // ith guardian

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
 * Calculate g^Pi(xcoord) mod p = Product ((K_i,j)^xcoord^j) mod p, j = 0, quorum-1.
 * Used to test secret key share by KeyCeremonyTrustee, and verifying results in TallyDecryptor.
 * spec 1.9, sec 3.2.2 eq 19:
 */
fun calculateGexpPiAtL(
    xcoord: Int,  // evaluated at xcoord ℓ
    coefficientCommitments: List<ElementModP>  // Kij for guardian i
): ElementModP {
    val group = compatibleContextOrFail(*coefficientCommitments.toTypedArray())
    val xcoordQ: ElementModQ = group.uIntToElementModQ(xcoord.toUInt())
    var result: ElementModP = group.ONE_MOD_P
    var xcoordPower: ElementModQ = group.ONE_MOD_Q // xcoord^j

    for (commitment in coefficientCommitments) {
        val term = commitment powP xcoordPower // (K_i,j)^ℓ^j
        result *= term
        xcoordPower *= xcoordQ
    }
    return result
}

/** Generate random coefficients for a polynomial of degree quorum-1. spec 1.9, p 19, eq 8 and 9. */
fun GroupContext.generatePolynomial(
    guardianId: String,
    guardianXCoord: Int,
    quorum: Int,
): ElectionPolynomial {
    val coefficients = mutableListOf<ElementModQ>()
    val commitments = mutableListOf<ElementModP>()
    val proofs = mutableListOf<SchnorrProof>()

    for (coeff in 0 until quorum) {
        val keypair: ElGamalKeypair = elGamalKeyPairFromRandom(this)
        coefficients.add(keypair.secretKey.key)
        commitments.add(keypair.publicKey.key)
        proofs.add(keypair.schnorrProof(guardianXCoord, coeff))
    }
    return ElectionPolynomial(guardianId, coefficients, commitments, proofs)
}

fun GroupContext.regeneratePolynomial(
    guardianId: String,
    guardianXCoord: Int,
    coefficients : List<ElementModQ>,
): ElectionPolynomial {
    val commitments = mutableListOf<ElementModP>()
    val proofs = mutableListOf<SchnorrProof>()

    var coeffIdx = 0
    coefficients.forEach { privateKey ->
        val publicKey = this.gPowP(privateKey)
        val keypair: ElGamalKeypair = ElGamalKeypair(ElGamalSecretKey(privateKey), ElGamalPublicKey(publicKey))
        commitments.add(keypair.publicKey.key)
        proofs.add(keypair.schnorrProof(guardianXCoord, coeffIdx))
        coeffIdx++
    }
    return ElectionPolynomial(guardianId, coefficients, commitments, proofs)
}