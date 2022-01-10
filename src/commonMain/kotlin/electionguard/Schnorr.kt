package electionguard

import mu.KotlinLogging
private val logger = KotlinLogging.logger {}

/**
 * Representation of a proof that the prover know the private key corresponding to the given public
 * key.
 */
data class SchnorrProof(
    val publicKey: ElGamalPublicKey,
    val commitment: ElementModP,
    val challenge: ElementModQ,
    val response: ElementModQ
)

/**
 * Given an ElGamal keypair (public and private key), and a random nonce, this generates a proof
 * that the author of the proof knew the public and corresponding private keys.
 */
fun ElGamalKeypair.schnorrProof(nonce: ElementModQ): SchnorrProof {
    val context = compatibleContextOrFail(publicKey, secretKey.e, nonce)
    val h = context.gPowP(nonce)
    val c = context.hashElements(publicKey, h)
    val u = nonce + secretKey.e * c

    return SchnorrProof(publicKey, h, c, u)
}

/**
 * Check validity of the proof for proving possession of the private key corresponding to the
 * `publicKey` field inside the proof.
 */
fun ElGamalPublicKey.hasValidSchnorrProof(proof: SchnorrProof): Boolean {
    val (k, h, challenge, u) = proof
    val context = compatibleContextOrFail(this, k, h, challenge, u)

    val validPublicKey = k.isValidResidue()
    val inBoundsH = h.inBounds()
    val inBoundsU = u.inBounds()
    val c = context.hashElements(k, h)
    val validChallenge = c == challenge
    val validProof = context.gPowP(u) == h * (k powP c)
    val samePublicKey = this == proof.publicKey
    val success =
        validPublicKey && inBoundsH && inBoundsU && validChallenge && validProof && samePublicKey

    if (!success) {
        val resultMap =
            mapOf(
                "inBoundsH" to inBoundsH,
                "inBoundsU" to inBoundsU,
                "validPublicKey" to validPublicKey,
                "validChallenge" to validChallenge,
                "validProof" to validProof,
                "samePublicKey" to samePublicKey,
                "proof" to this
            )
        logger.warn { "found an invalid Schnorr proof: $resultMap" }
    }

    return success
}