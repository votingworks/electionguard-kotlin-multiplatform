package electionguard.core

import mu.KotlinLogging
private val logger = KotlinLogging.logger("Schnorr")

/**
 * Representation of a proof that the prover know the private key corresponding to the given public
 * key. (The public key is not included, to keep the proof small, but the first four (most significant)
 * bytes are kept around as a checksum.)
 */
data class SchnorrProof(
    val publicKeyChecksum: ByteArray,
    val challenge: ElementModQ,
    val response: ElementModQ
)

/**
 * Given an ElGamal keypair (public and private key), and the crypto base hash (Q), this generates a proof
 * that the author of the proof knew the public and corresponding private keys. This proof is deterministically
 * generated based on the randomness provided by `nonce`, so don't use the same nonce twice, or if the argument
 * is not specified, its default value is chosen at random.
 */
fun ElGamalKeypair.schnorrProof(
    cryptoBaseHash: ElementModQ,
    nonce: ElementModQ = context.randomElementModQ()
): SchnorrProof {
    val context = compatibleContextOrFail(publicKey.key, secretKey.key, nonce, cryptoBaseHash)
    val h = context.gPowP(nonce)
    val c = hashElements(cryptoBaseHash, publicKey, h).toElementModQ(context)
    val u = nonce + secretKey.key * c

    val pkChecksumBytes = publicKey.key.byteArray().copyOfRange(0, 4)

    return SchnorrProof(pkChecksumBytes, c, u)
}

/**
 * Check validity of the proof for proving possession of the private key corresponding to the
 * given public key (i.e., `this` public key).
 */
fun ElGamalPublicKey.hasValidSchnorrProof(cryptoBaseHash: ElementModQ, proof: SchnorrProof): Boolean {
    val (checksum, challenge, u) = proof
    val context = compatibleContextOrFail(this.key, challenge, u)

    val matchingChecksum = this.key.byteArray().copyOfRange(0, 4).contentEquals(checksum)
    val inBoundsU = u.inBounds()

    val gPowU = context.gPowP(u)
    val h = gPowU / this.powP(challenge)
    val c = hashElements(cryptoBaseHash, this, h).toElementModQ(context)

    val validChallenge = c == challenge

    val success = matchingChecksum && inBoundsU && validChallenge

    if (!success) {
        val resultMap =
            mapOf(
                "inBoundsU" to inBoundsU,
                "matchingChecksum" to matchingChecksum,
                "validChallenge" to validChallenge,
                "proof" to this
            )
        logger.warn { "found an invalid Schnorr proof: $resultMap" }
    }

    return success
}