package electionguard.core

import com.github.michaelbull.result.*
import mu.KotlinLogging
private val logger = KotlinLogging.logger("SchnorrProof")

/**
 * Proof that the prover knows the private key corresponding to the public key.
 */
data class SchnorrProof(
    val publicKey: ElementModP, // K_ij
    val challenge: ElementModQ,
    val response: ElementModQ) {

    init {
        compatibleContextOrFail(publicKey, challenge, response)
    }

    fun validate(): Result<Boolean, String> {
        val context = compatibleContextOrFail(publicKey, challenge, response)

        val gPowV = context.gPowP(response) // g^v_ij
        val h = gPowV * (publicKey powP challenge) // h_ij; spec 1.52, section 3.2.2, eq 2.1
        val c = hashElements(publicKey, h).toElementModQ(context)

        val inBoundsU = response.inBounds()
        val validChallenge = c == challenge // 2.A
        val success = inBoundsU && validChallenge

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
 * that the author of the proof knew the private key, without revealing it.
 */
fun ElGamalKeypair.schnorrProof(
    nonce: ElementModQ = context.randomElementModQ()
): SchnorrProof {
    // spec 1.52, section 3.2.2, eq 7, 8
    val context = compatibleContextOrFail(publicKey.key, secretKey.key, nonce)
    val h = context.gPowP(nonce) // eq 7
    val c = hashElements(publicKey, h).toElementModQ(context) // 2.A, eq 8
    val v = nonce - secretKey.key * c

    return SchnorrProof(publicKey.key, c, v)
}