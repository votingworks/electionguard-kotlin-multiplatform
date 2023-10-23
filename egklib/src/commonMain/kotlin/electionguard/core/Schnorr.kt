package electionguard.core

import com.github.michaelbull.result.*
import mu.KotlinLogging
private val logger = KotlinLogging.logger("SchnorrProof")

/**
 * Proof that the prover knows the private key corresponding to the public key.
 * Spec 2.0.0, section 3.2.2, "NIZK Proof" (non-interactive zero knowledge proof).
 */
data class SchnorrProof(
    val publicKey: ElementModP, // K_ij, public commitment to the jth coefficient.
    val challenge: ElementModQ, // c_ij, p 22. eq 12
    val response: ElementModQ,  // v_ij = nonce - secretKey.key * cij.
    ) {

    init {
        compatibleContextOrFail(publicKey, challenge, response)
        require(publicKey.isValidResidue()) // 2.A
    }

    // verification Box 2, p 23
    fun validate(guardianXCoord: Int, coeff: Int): Result<Boolean, String> {
        val group = compatibleContextOrFail(publicKey, challenge, response)

        val gPowV = group.gPowP(response)
        val h = gPowV * (publicKey powP challenge) // h_ij (2.1)
        // h = g^v * K^c = g^(u - c*s) * g^s*c = g^(u - c*s + c*s) = g^u
        val c = hashFunction(group.constants.hp.bytes, 0x10.toByte(), guardianXCoord, coeff, publicKey, h).toElementModQ(group) // 2.C
        // c wouldnt agree unless h = g^u
        // therefore, whoever generated v knows s

        val inBoundsK = publicKey.isValidResidue() // 2.A
        val inBoundsU = response.inBounds() // 2.B
        val validChallenge = c == challenge // 2.C
        val success = inBoundsK && inBoundsU && validChallenge

        if (!success) {
            val resultMap =
                mapOf(
                    "inBoundsU" to inBoundsU,
                    "validChallenge" to validChallenge,
                    "proof" to this
                )
            logger.warn { "found an invalid Schnorr proof: $resultMap" }
            return Err("inBoundsU=$inBoundsU validChallenge=$validChallenge")
        }

        return Ok(true)
    }
}

/**
 * Given an ElGamal keypair (public and private key) for i,j, generate a ZNP (zero knowledge proof)
 * that the author of the proof knows the private key, without revealing it.
 */
fun ElGamalKeypair.schnorrProof(
    guardianXCoord: Int, // i
    coeff: Int, // j
    nonce: ElementModQ = context.randomElementModQ() // u_ij
): SchnorrProof {
    val context = compatibleContextOrFail(publicKey.key, secretKey.key, nonce)
    val h = context.gPowP(nonce) // h = g ^ u; spec 2.0.0,  eq 11
    val c = hashFunction(context.constants.hp.bytes, 0x10.toByte(), guardianXCoord, coeff, publicKey.key, h).toElementModQ(context) // eq 12
    val v = nonce - secretKey.key * c

    return SchnorrProof(publicKey.key, c, v)
}