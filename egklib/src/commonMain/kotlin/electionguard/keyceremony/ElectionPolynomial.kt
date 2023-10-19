package electionguard.keyceremony

import electionguard.core.*

/** Pi(x), spec 2.0.0, section 3.2.1. Must be kept secret. */
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

        // spec 2.0.0, p 22, eq 9
        for (coefficient in this.coefficients) {
            val term = coefficient * xcoordPower
            result += term
            xcoordPower *= xcoordQ
        }
        return result
    }
}

/**
 * Calculate g^Pi(xcoord) mod p
 * Used to test secret key share by KeyCeremonyTrustee, and verify results in TallyDecryptor.
 * spec 2.0.0, sec 3.2.2, p 24, eq 21
 *
 * Also see spec 2.0, eq 13:
 * g^Pi(x) mod p =
 *    g^( Sum_j ( a_i,j * x^j), j=0..k-1 ) mod p =
 *    Prod_j (g^(a_i,j * x^j),  j=0..k-1 ) mod p =
 *    Prod_j (g^(a_i,j)^(x^j)), j=0..k-1 ) mod p =
 *    Prod_j ((K_i,j)^(x^j)),   j=0..k-1 ) mod p
 */
fun calculateGexpPiAtL(
    guardianId: String, // debug
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
    // println(" calculateGexpPiAtL for $guardianId at xcoord = $xcoord exps = ${coefficientCommitments.size}")
    return result
}

/** Generate random coefficients for a polynomial of degree quorum-1. spec 2.0.0, p 22, eq 9 and 10. */
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

// possibly only used in testing?
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
        val keypair = ElGamalKeypair(ElGamalSecretKey(privateKey), ElGamalPublicKey(publicKey))
        commitments.add(keypair.publicKey.key)
        proofs.add(keypair.schnorrProof(guardianXCoord, coeffIdx))
        coeffIdx++
    }
    return ElectionPolynomial(guardianId, coefficients, commitments, proofs)
}