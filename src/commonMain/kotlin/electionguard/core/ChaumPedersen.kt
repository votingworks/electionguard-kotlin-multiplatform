package electionguard.core

import mu.KotlinLogging
private val logger = KotlinLogging.logger("ChaumPedersen")

/** Proof that the ciphertext is a given constant. */
data class ConstantChaumPedersenProofKnownNonce(
    val proof: GenericChaumPedersenProof,
    val constant: Int
)

/** Proof that the ciphertext is a given constant. */
data class ConstantChaumPedersenProofKnownSecretKey(
    val proof: GenericChaumPedersenProof,
    val constant: Int
)

/**
 * Proof that the ciphertext is either zero or one. (See
 * [Cramer-Damgård-Schoenmakers 1994](https://www.iacr.org/cryptodb/data/paper.php?pubkey=1194))
 */
data class DisjunctiveChaumPedersenProofKnownNonce(
    val proof0: GenericChaumPedersenProof,
    val proof1: GenericChaumPedersenProof,
    val c: ElementModQ
)

/**
 * General-purpose Chaum-Pedersen proof object, demonstrating that the prover knows the exponent `x`
 * for two tuples `(g, g^x)` and `(h, h^x)`, without revealing anything about `x`. This is used as a
 * component in other proofs. (See
 * [Chaum-Pedersen 1992](https://link.springer.com/chapter/10.1007/3-540-48071-4_7))
 *
 * @param c hash(a, b, and possibly other state) (aka challenge)
 * @param r w + xc (aka response)
 */
data class GenericChaumPedersenProof(val c: ElementModQ, val r: ElementModQ)

/**
 * Expanded form of the [GenericChaumPedersenProof], with the `a` and `b` values recomputed. This
 * should not be serialized.
 */
internal data class ExpandedGenericChaumPedersenProof(
    val a: ElementModP,
    val b: ElementModP,
    val c: ElementModQ,
    val r: ElementModQ,
)

/**
 * Given a [GenericChaumPedersenProof], computes the `a` and `b` values that are needed for proofs
 * and such, but are removed for serialization.
 */
internal fun GenericChaumPedersenProof.expand(
    g: ElementModP,
    gx: ElementModP,
    h: ElementModP,
    hx: ElementModP,
): ExpandedGenericChaumPedersenProof {
    val negC = -c
    val gr = g powP r // g^r = g^{w + xc}
    val hr = h powP r // h^r = h^{w + xc}
    val a = gr * (gx powP negC) // cancelling out the xc, getting g^w
    val b = hr * (hx powP negC) // cancelling out the xc, getting h^w
    return ExpandedGenericChaumPedersenProof(a, b, c, r)
}

/**
 * Produces a proof that a given ElGamal encryption corresponds to a specific total value. This
 * requires the prover to know the nonce (the r behind the g^r).
 *
 * @param plaintext The plaintext constant value used to make the ElGamal ciphertext (L in the spec)
 * @param nonce The aggregate nonce used creating the ElGamal ciphertext (r in the spec)
 * @param publicKey The ElGamal public key for the election
 * @param seed Used to generate other random values here
 * @param hashHeader A value used when generating the challenge, usually the election extended base
 *     hash (Q')
 */
fun ElGamalCiphertext.constantChaumPedersenProofKnownNonce(
    plaintext: Int,
    nonce: ElementModQ,
    publicKey: ElGamalPublicKey,
    seed: ElementModQ,
    hashHeader: ElementModQ
): ConstantChaumPedersenProofKnownNonce {
    val context = compatibleContextOrFail(pad, nonce, publicKey.key, seed, hashHeader)
    return ConstantChaumPedersenProofKnownNonce(
        genericChaumPedersenProofOf(
            g = context.G_MOD_P,
            h = publicKey.key,
            x = nonce,
            seed = seed,
            alsoHash = arrayOf(pad, data),
            hashHeader = hashHeader
        ),
        plaintext
    )
}

/**
 * Produces a proof that a given ElGamal encryption corresponds to a specific total value. This
 * requires the prover to know the secret ElGamal encryption key.
 *
 * @param plaintext The plaintext constant value used to make the ElGamal ciphertext (L in the spec)
 * @param keypair The ElGamal secret and public key pair for the election
 * @param seed Used to generate other random values here
 * @param hashHeader A value used when generating the challenge, usually the election extended base
 *     hash (Q')
 */
fun ElGamalCiphertext.constantChaumPedersenProofKnownSecretKey(
    plaintext: Int,
    keypair: ElGamalKeypair,
    seed: ElementModQ,
    hashHeader: ElementModQ
) : ConstantChaumPedersenProofKnownSecretKey {
    val context = compatibleContextOrFail(pad, keypair.secretKey.key, seed, hashHeader)
    return ConstantChaumPedersenProofKnownSecretKey(
        genericChaumPedersenProofOf(
            g = context.G_MOD_P,
            h = pad,
            x = keypair.secretKey.key,
            seed = seed,
            alsoHash = arrayOf(pad, data),
            hashHeader = hashHeader
        ),
        plaintext
    )
}

/**
 * Produces a proof that a given ElGamal encryption corresponds to either zero or one. This requires
 * the prover to know the nonce (the r behind the g^r).
 *
 * @param plaintext The actual plaintext constant value used to make the ElGamal ciphertext (L in
 *     the spec)
 * @param nonce The aggregate nonce used creating the ElGamal ciphertext (r in the spec)
 * @param publicKey The ElGamal public key for the election
 * @param seed Used to generate other random values here
 * @param hashHeader A value used when generating the challenge, usually the election extended base
 *     hash (Q')
 */
fun ElGamalCiphertext.disjunctiveChaumPedersenProofKnownNonce(
    plaintext: Int,
    nonce: ElementModQ,
    publicKey: ElGamalPublicKey,
    seed: ElementModQ,
    hashHeader: ElementModQ
): DisjunctiveChaumPedersenProofKnownNonce {
    val context = compatibleContextOrFail(pad, nonce, publicKey.key, seed, hashHeader)
    return when (plaintext) {
        0 -> {
            val (alpha, beta) = this
            val (u, v, w) = Nonces(seed, "disjoint-chaum-pedersen-proof")

            val a0 = context.gPowP(u)
            val b0 = publicKey powP u
            val a1 = context.gPowP(v)
            val b1 = publicKey powP (w + v)
            val c = hashElements(hashHeader, alpha, beta, a0, b0, a1, b1).toElementModQ(context)
            val c0 = c - w
            val c1 = w
            val v0 = u + c0 * nonce
            val v1 = v + c1 * nonce

            val realZeroProof = GenericChaumPedersenProof(c0, v0)
            val fakeOneProof = GenericChaumPedersenProof(c1, v1)

            DisjunctiveChaumPedersenProofKnownNonce(realZeroProof, fakeOneProof, c)
        }
        1 -> {
            val (alpha, beta) = this
            val (u, v, w) = Nonces(seed, "disjoint-chaum-pedersen-proof")

            val a0 = context.gPowP(v)
            val b0 = publicKey powP (w + v)
            val a1 = context.gPowP(u)
            val b1 = publicKey powP u
            val c = hashElements(hashHeader, alpha, beta, a0, b0, a1, b1).toElementModQ(context)
            val c0 = -w
            val c1 = c + w
            val v0 = v + c0 * nonce
            val v1 = u + c1 * nonce

            val fakeZeroProof = GenericChaumPedersenProof(c0, v0)
            val realOneProof = GenericChaumPedersenProof(c1, v1)

            DisjunctiveChaumPedersenProofKnownNonce(fakeZeroProof, realOneProof, c)
        }
        else ->
            throw ArithmeticException(
                "cannot compute disjunctive c-p proof with constant = $plaintext"
            )
    }
}

/**
 * Validates a proof against an ElGamal ciphertext.
 *
 * @param ciphertext An ElGamal ciphertext
 * @param publicKey The public key of the election
 * @param hashHeader A value used when generating the challenge, usually the election extended base
 *     hash (Q')
 * @param expectedConstant Optional parameter. If specified, the constant in the proof is validated
 *     against the expected constant.
 * @return true if the proof is valid
 */
fun ConstantChaumPedersenProofKnownNonce.isValid(
    ciphertext: ElGamalCiphertext,
    publicKey: ElGamalPublicKey,
    hashHeader: ElementModQ,
    expectedConstant: Int = -1
) : Boolean {
    val context = compatibleContextOrFail(proof.c, ciphertext.pad, hashHeader)

    val constantQ = -(constant.toElementModQ(context))
    return proof.isValid(
        g = context.G_MOD_P,
        gx = ciphertext.pad,
        h = publicKey.key,
        hx = ciphertext.data * (publicKey powP constantQ),
        alsoHash = arrayOf(ciphertext.pad, ciphertext.data),
        hashHeader = hashHeader,
        checkC = true,
    ) and (if (expectedConstant != -1) constant == expectedConstant else true)
}

/**
 * Validates a proof against an ElGamal ciphertext.
 *
 * @param ciphertext An ElGamal ciphertext
 * @param publicKey The public key of the election
 * @param hashHeader A value used when generating the challenge, usually the election extended base
 *     hash (Q')
 * @param expectedConstant Optional parameter. If specified, the constant in the proof is validated
 *     against the expected constant.
 * @return true if the proof is valid
 */
fun ConstantChaumPedersenProofKnownSecretKey.isValid(
    ciphertext: ElGamalCiphertext,
    publicKey: ElGamalPublicKey,
    hashHeader: ElementModQ,
    expectedConstant: Int = -1
) : Boolean {
    val context = compatibleContextOrFail(proof.c, ciphertext.pad, publicKey.key, hashHeader)

    val constantQ = -(constant.toElementModQ(context))
    return proof.isValid(
        g = context.G_MOD_P,
        gx = publicKey.key,
        h = ciphertext.pad,
        hx = ciphertext.data * (publicKey powP constantQ),
        alsoHash = arrayOf(ciphertext.pad, ciphertext.data),
        hashHeader = hashHeader,
        checkC = true
    ) and (if (expectedConstant != -1) constant == expectedConstant else true)
}

/**
 * Validates a proof against an ElGamal ciphertext.
 *
 * @param ciphertext An ElGamal ciphertext
 * @param publicKey The public key of the election
 * @param hashHeader A value used when generating the challenge, usually the election extended base
 *     hash (Q')
 * @return true if the proof is valid
 */
fun DisjunctiveChaumPedersenProofKnownNonce.isValid(
    ciphertext: ElGamalCiphertext,
    publicKey: ElGamalPublicKey,
    hashHeader: ElementModQ
): Boolean {
    val context = compatibleContextOrFail(c, ciphertext.pad, publicKey.key, hashHeader)

    val (alpha, beta) = ciphertext
    val consistentC = proof0.c + proof1.c == c
    val eproof0 =
        proof0.expand(
            g = context.G_MOD_P,
            gx = ciphertext.pad,
            h = publicKey.key,
            hx = ciphertext.data,
        )
    val eproof1 =
        proof1.expand(
            g = context.G_MOD_P,
            gx = ciphertext.pad,
            h = publicKey.key,
            hx = ciphertext.data * publicKey.inverseKey
        )

    val validHash =
        c ==
            hashElements(hashHeader, alpha, beta, eproof0.a, eproof0.b, eproof1.a, eproof1.b)
                .toElementModQ(context)

    val valid0 =
        eproof0.isValid(
            g = context.G_MOD_P,
            gx = ciphertext.pad,
            h = publicKey.key,
            hx = ciphertext.data,
            checkC = false,
            hashHeader = hashHeader,
        )

    val valid1 =
        eproof1.isValid(
            g = context.G_MOD_P,
            gx = ciphertext.pad,
            h = publicKey.key,
            hx = ciphertext.data * publicKey.inverseKey,
            checkC = false,
            hashHeader = hashHeader,
        )

    // If valid0 or valid1 is false, this will already have been logged,
    // so we don't have to repeat it here.
    if (!consistentC || !validHash)
        logger.warn {
            "Invalid commitments for disjunctive Chaum-Pedersen proof: " +
                mapOf(
                    "consistentC" to consistentC,
                    "validHash" to validHash,
                    "valid0" to valid0,
                    "valid1" to valid1
                ).toString()
        }

    return valid0 && valid1 && consistentC && validHash
}

/**
 * Checks that this Chaum-Pedersen proof certifies that the prover knew an x, such that (g, g^x) and
 * (h, h^x) share the same exponent x, without revealing x. Part of the proof is a challenge
 * constant. By suppressing this check, "fake" proofs can be validated. Useful when doing
 * disjunctive proofs.
 *
 * @param g See above.
 * @param gx See above.
 * @param h See above.
 * @param hx See above.
 * @param hashHeader A value used when generating the challenge, usually the election extended base
 *     hash (Q')
 * @param alsoHash Optional additional values to include in the hash challenge computation hash
 * @param checkC If false, the challenge constant is not verified. (default: true)
 * @return true if the proof is valid
 */
fun GenericChaumPedersenProof.isValid(
    g: ElementModP,
    gx: ElementModP,
    h: ElementModP,
    hx: ElementModP,
    hashHeader: ElementModQ,
    alsoHash: Array<Element> = emptyArray(),
    checkC: Boolean = true,
): Boolean {
    return expand(g, gx, h, hx).isValid(g, gx, h, hx, hashHeader, alsoHash, checkC)
}

internal fun ExpandedGenericChaumPedersenProof.isValid(
    g: ElementModP,
    gx: ElementModP,
    h: ElementModP,
    hx: ElementModP,
    hashHeader: ElementModQ,
    alsoHash: Array<Element> = emptyArray(),
    checkC: Boolean = true
): Boolean {
    val context = compatibleContextOrFail(c, g, gx, h, hx, hashHeader)

    val inBoundsG = g.isValidResidue()
    val inBoundsGx = gx.isValidResidue()
    val inBoundsH = h.isValidResidue()
    val inBoundsHx = hx.isValidResidue()

    val hashGood = !checkC || c == hashElements(hashHeader, a, b, *alsoHash).toElementModQ(context)

    val success = (hashGood && inBoundsG && inBoundsGx && inBoundsH && inBoundsHx)

    if (!success)
        logger.warn {
            "Invalid generic Chaum-Pedersen proof: " +
                mapOf(
                    "hashGood" to hashGood,
                    "inBoundsG" to inBoundsG,
                    "inBoundsGx" to inBoundsGx,
                    "inBoundsH" to inBoundsH,
                    "inBoundsHx" to inBoundsHx,
                ).toString()
        }

    return success
}

/**
 * Produces a generic Chaum-Pedersen proof that two tuples share an exponent, i.e., that for (g,
 * g^x) and (h, h^x), it's the same value of x, but without revealing x. This generic proof can be
 * used as a building-block for many other proofs.
 *
 * There's no need for g^x and h^x in this particular computation.
 *
 * @param g Any element in P that can be generated by [GroupContext.gPowP]
 * @param h Any element in P that can be generated by [GroupContext.gPowP]
 * @param x Any element in Q
 * @param seed Used to randomize the generation of the Chaum-Pedersen proof.
 * @param hashHeader A value used when generating the challenge, usually the election extended base
 *     hash (Q')
 * @param alsoHash Optional additional values to include in the hash challenge computation hash.
 */
fun genericChaumPedersenProofOf(
    g: ElementModP,
    h: ElementModP,
    x: ElementModQ,
    seed: ElementModQ,
    hashHeader: ElementModQ,
    alsoHash: Array<Element> = emptyArray()
): GenericChaumPedersenProof {
    val context = compatibleContextOrFail(g, h, x, seed, hashHeader)

    val w = Nonces(seed, "generic-chaum-pedersen-proof")[0]
    val a = g powP w
    val b = h powP w

    val c = hashElements(hashHeader, a, b, *alsoHash).toElementModQ(context)
    val r = w + x * c

    return GenericChaumPedersenProof(c, r)
}

/**
 * Produces a generic "fake" Chaum-Pedersen proof that two tuples share an exponent, i.e., that for
 * (g, g^x) and (h, h^x), it's the same value of x, but without revealing x. Unlike the regular
 * Chaum-Pedersen proof, this version allows the challenge `c` to be specified, which allows
 * everything to be faked. See the [GenericChaumPedersenProof.isValid] method on the resulting proof
 * object. By default, the challenge is validated by hashing elements of the proof, which prevents
 * these "fake" proofs from passing validation, but you can suppress the hash check with an optional
 * parameter.
 *
 * The seed is used for generating the random numbers used in the proof. Otherwise everything is
 * completely deterministic.
 *
 * Note that there's no need to specify g, gx, h, and hx, since those values are completely
 * unnecessary to produce a fake proof of their correspondence!
 */
fun fakeGenericChaumPedersenProofOf(c: ElementModQ, seed: ElementModQ): GenericChaumPedersenProof {
    compatibleContextOrFail(c, seed)
    val r = Nonces(seed, "generic-chaum-pedersen-proof")[0]
    return GenericChaumPedersenProof(c, r)
}