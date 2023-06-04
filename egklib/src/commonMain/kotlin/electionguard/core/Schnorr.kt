package electionguard.core

import com.github.michaelbull.result.*
import mu.KotlinLogging
private val logger = KotlinLogging.logger("SchnorrProof")

/**
 * Proof that the prover knows the private key corresponding to the public key.
 * Spec 1.9, section 3.2.2, NIZK Proof (non-interactive zero knowledge proof).
 */
data class SchnorrProof(
    val publicKey: ElementModP, // K_ij, public commitment to the jth coefficient, spec 1.52, eq 6.
    val challenge: ElementModQ,
    val response: ElementModQ) {

    init {
        compatibleContextOrFail(publicKey, challenge, response)
        require(publicKey.isValidResidue()) // 2.A
    }

    // verification Box 2, p 20
    fun validate(guardianXCoord: Int, coeff: Int): Result<Boolean, String> {
        val context = compatibleContextOrFail(publicKey, challenge, response)

        val gPowV = context.gPowP(response) // g^v_ij
        val h = gPowV * (publicKey powP challenge) // h_ij (2.1)
        val c = hashFunction(context.constants.hp.bytes, 0x10.toByte(), guardianXCoord.toUShort(), coeff.toUShort(), publicKey, h).toElementModQ(context) // 2.C

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
 * Given an ElGamal keypair (public and private key), generate a ZNP (zero knowledge proof)
 * that the author of the proof knows the private key, without revealing it.
 */
fun ElGamalKeypair.schnorrProof(
    guardianXCoord: Int, // i
    coeff: Int, // j
    nonce: ElementModQ = context.randomElementModQ() // u_ij
): SchnorrProof {
    // spec 1.9, p 19
    val context = compatibleContextOrFail(publicKey.key, secretKey.key, nonce)
    val h = context.gPowP(nonce) // eq 10
    val c = hashFunction(context.constants.hp.bytes, 0x10.toByte(), guardianXCoord.toUShort(), coeff.toUShort(), publicKey.key, h).toElementModQ(context) // eq 11
    val v = nonce - secretKey.key * c // response

    return SchnorrProof(publicKey.key, c, v)
}