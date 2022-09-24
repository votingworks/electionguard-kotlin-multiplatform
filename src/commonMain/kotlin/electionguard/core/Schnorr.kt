package electionguard.core

import mu.KotlinLogging
private val logger = KotlinLogging.logger("Schnorr")

/**
 * Proof that the prover knows the private key corresponding to the public key.
 */
data class SchnorrProof(
    val publicKey: ElementModP,
    val challenge: ElementModQ,
    val response: ElementModQ) {

    init {
        compatibleContextOrFail(publicKey, challenge, response)
    }

    fun isValid(): Boolean {
        val context = compatibleContextOrFail(publicKey, challenge, response)

        val gPowV = context.gPowP(response)
        val h = gPowV * (publicKey powP challenge)
        val c = hashElements(publicKey, h).toElementModQ(context)

        val inBoundsU = response.inBounds()
        val validChallenge = c == challenge
        val success = inBoundsU && validChallenge

        if (!success) {
            val resultMap =
                mapOf(
                    "inBoundsU" to inBoundsU,
                    "validChallenge" to validChallenge,
                    "proof" to this
                )
            logger.warn { "found an invalid Schnorr proof: $resultMap" }
        }

        return success
    }
}

/**
 * Given an ElGamal keypair (public and private key), generate a ZNP (zero knowledge proof)
 * that the author of the proof knew the private key, without revealing it.
 */
fun ElGamalKeypair.schnorrProof(
    nonce: ElementModQ = context.randomElementModQ()
): SchnorrProof {
    // spec 1.51, section 3.2.2, eq 10, 11
    val context = compatibleContextOrFail(publicKey.key, secretKey.key, nonce)
    val h = context.gPowP(nonce)
    val c = hashElements(publicKey, h).toElementModQ(context)
    val v = nonce - secretKey.key * c

    return SchnorrProof(publicKey.key, c, v)
}