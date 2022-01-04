package electionguard

// Reference specification:
// https://github.com/microsoft/electionguard/releases/tag/v0.95.0

/** This should really use a logging library, but we don't have one right now... */
private fun logWarning(
    @Suppress("UNUSED_PARAMETER")
    f: () -> String
) {
    //    println(f())
}

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
 * @param a g^w
 * @param b h^w
 * @param c hash(a, b, and possibly other state)
 * @param r w + xc
 */
data class GenericChaumPedersenProof(
    val a: ElementModP,
    val b: ElementModP,
    val c: ElementModQ,
    val r: ElementModQ
)

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
    publicKey: ElementModP,
    seed: ElementModQ,
    hashHeader: ElementModQ
): ConstantChaumPedersenProofKnownNonce {
    val context = compatibleContextOrFail(this.data, this.pad, nonce, publicKey, seed, hashHeader)
    return ConstantChaumPedersenProofKnownNonce(
        genericChaumPedersenProofOf(
            g = context.G_MOD_P,
            h = publicKey,
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
    val context =
        compatibleContextOrFail(
            this.data,
            this.pad,
            keypair.secretKey.e,
            keypair.publicKey,
            seed,
            hashHeader
        )
    return ConstantChaumPedersenProofKnownSecretKey(
        genericChaumPedersenProofOf(
            g = context.G_MOD_P,
            h = pad,
            x = keypair.secretKey.e,
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
    publicKey: ElementModP,
    seed: ElementModQ,
    hashHeader: ElementModQ
): DisjunctiveChaumPedersenProofKnownNonce {
    val context = compatibleContextOrFail(this.data, this.pad, nonce, publicKey, seed, hashHeader)
    return when (plaintext) {
        0 -> {
            // Note: this is using Benaloh's revised equations ("Efficient Implementation of
            // ElectionGuard Ballot Encryption and Proofs", 2021).

            val (alpha, beta) = this
            val (u, v, w) = Nonces(seed, "disjoint-chaum-pedersen-proof")

            val a0 = context.gPowP(u)
            val b0 = publicKey powP u
            val a1 = context.gPowP(v)
            val b1 = context.gPowP(w) * (publicKey powP v)
            val c = context.hashElements(hashHeader, alpha, beta, a0, b0, a1, b1)
            val c0 = c - w
            val c1 = w
            val v0 = u + c0 * nonce
            val v1 = v + c1 * nonce

            val realZeroProof = GenericChaumPedersenProof(a0, b0, c0, v0)
            val fakeOneProof = GenericChaumPedersenProof(a1, b1, c1, v1)

            DisjunctiveChaumPedersenProofKnownNonce(realZeroProof, fakeOneProof, c)
        }
        1 -> {
            // Note: this is using Benaloh's revised equations ("Efficient Implementation of
            // ElectionGuard Ballot Encryption and Proofs", 2021).
            //
            //    # Pick three random numbers in Q.
            //    u, v, w = Nonces(seed, "disjoint-chaum-pedersen-proof")[0:3]
            //
            //    # Compute the NIZKP
            //    a0 = g_pow_p(v)
            //    b0 = mult_p(g_pow_p(w), k.pow_p(v))
            //    a1 = g_pow_p(u)
            //    b1 = k.pow_p(u)
            //    c = hash_elems(q, alpha, beta, a0, b0, a1, b1)
            //    c0 = negate_q(w)
            //    c1 = add_q(c, w)
            //    v0 = a_plus_bc_q(v, c0, r)
            //    v1 = a_plus_bc_q(u, c1, r)

            val (alpha, beta) = this
            val (u, v, w) = Nonces(seed, "disjoint-chaum-pedersen-proof")

            val a0 = context.gPowP(v)
            val b0 = context.gPowP(w) * (publicKey powP v)
            val a1 = context.gPowP(u)
            val b1 = publicKey powP u
            val c = context.hashElements(hashHeader, alpha, beta, a0, b0, a1, b1)
            val c0 = -w
            val c1 = c + w
            val v0 = v + c0 * nonce
            val v1 = u + c1 * nonce

            val fakeZeroProof = GenericChaumPedersenProof(a0, b0, c0, v0)
            val realOneProof = GenericChaumPedersenProof(a1, b1, c1, v1)

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
 */
fun ConstantChaumPedersenProofKnownNonce.isValid(
    ciphertext: ElGamalCiphertext,
    publicKey: ElementModP,
    hashHeader: ElementModQ,
    expectedConstant: Int = -1
) : Boolean {
    val context =
        compatibleContextOrFail(
            this.proof.a,
            ciphertext.data,
            ciphertext.pad,
            publicKey,
            hashHeader
        )
    return proof.isValid(
        g = context.G_MOD_P,
        gx = ciphertext.pad,
        h = publicKey,
        hx = ciphertext.data * context.gPowP(-constant.toElementModQ(context)),
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
 * @param expectedConstant Optional parameter. If specified, the constant in the proof is validated
 *     against the expected constant.
 */
fun ConstantChaumPedersenProofKnownSecretKey.isValid(
    ciphertext: ElGamalCiphertext,
    publicKey: ElementModP,
    hashHeader: ElementModQ,
    expectedConstant: Int = -1
) : Boolean {
    val context =
        compatibleContextOrFail(
            this.proof.a,
            ciphertext.data,
            ciphertext.pad,
            publicKey,
            hashHeader
        )
    return proof.isValid(
        g = context.G_MOD_P,
        gx = publicKey,
        h = ciphertext.pad,
        hx = ciphertext.data * context.gPowP(-constant.toElementModQ(context)),
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
 */
fun DisjunctiveChaumPedersenProofKnownNonce.isValid(
    ciphertext: ElGamalCiphertext,
    publicKey: ElementModP,
    hashHeader: ElementModQ
): Boolean {
    val context =
        compatibleContextOrFail(this.c, ciphertext.data, ciphertext.pad, publicKey, hashHeader)
    val (alpha, beta) = ciphertext
    val consistentC = proof0.c + proof1.c == c
    val validHash =
        c == context.hashElements(hashHeader, alpha, beta, proof0.a, proof0.b, proof1.a, proof1.b)

    val valid0 =
        proof0.isValid(
            g = context.G_MOD_P,
            gx = ciphertext.pad,
            h = publicKey,
            hx = ciphertext.data,
            checkC = false,
            hashHeader = hashHeader,
        )

    val valid1 =
        proof1.isValid(
            g = context.G_MOD_P,
            gx = ciphertext.pad,
            h = publicKey,
            hx = ciphertext.data * context.GINV_MOD_P,
            checkC = false,
            hashHeader = hashHeader,
        )

    // If valid0 or valid1 is false, this will already have been logged,
    // so we don't have to repeat it here.
    if (!consistentC || !validHash)
        logWarning {
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
    checkC: Boolean = true
): Boolean {
    val context = compatibleContextOrFail(this.a, g, gx, h, hx, hashHeader, *alsoHash)
    val inBoundsA = a.isValidResidue()
    val inBoundsB = b.isValidResidue()
    val inBoundsG = g.isValidResidue()
    val inBoundsGx = gx.isValidResidue()
    val inBoundsH = h.isValidResidue()
    val inBoundsHx = hx.isValidResidue()

    val hashGood = !(checkC) || (c == context.hashElements(hashHeader, a, b, *alsoHash))

    val agxc = a * (gx powP c) // should yield g^{w + xc}
    val gr = g powP r // should also yield g^{w + xc}

    val goodG = agxc == gr

    val bhxc = b * (hx powP c)
    val hr = h powP r

    val goodH = bhxc == hr

    val success =
        (hashGood && inBoundsA && inBoundsB && inBoundsG && inBoundsGx && inBoundsH && inBoundsHx &&
            goodG && goodH)

    if (!success)
        logWarning {
            "Invalid generic Chaum-Pedersen proof: " +
                mapOf(
                    "hash_good" to hashGood,
                    "in_bounds_a" to inBoundsA,
                    "in_bounds_b" to inBoundsB,
                    "in_bounds_g" to inBoundsG,
                    "in_bounds_gx" to inBoundsGx,
                    "in_bounds_h" to inBoundsH,
                    "in_bounds_hx" to inBoundsHx,
                    "good_g" to goodG,
                    "good_h" to goodH,
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
 * @param g Any element in P that can be generated by [gPowP]
 * @param h Any element in P that can be generated by [gPowP]
 * @param x Any element in Q
 * @param seed Used to randomize the generation of the Chaum-Pedersen proof.
 * @param hashHeader A value used when generating the challenge, usually the election extended base
 *     hash (Q')
 * @param alsoHash Optional additional values to include in the hash challenge computation hash.
 *     Ignored when [c] is specified.
 */
fun genericChaumPedersenProofOf(
    g: ElementModP,
    h: ElementModP,
    x: ElementModQ,
    seed: ElementModQ,
    hashHeader: ElementModQ,
    alsoHash: Array<Element> = emptyArray()
): GenericChaumPedersenProof {
    val context = compatibleContextOrFail(g, h, x, seed, hashHeader, *alsoHash)
    val w = Nonces(seed, "generic-chaum-pedersen-proof")[0]
    val a = g powP w
    val b = h powP w

    val c = context.hashElements(hashHeader, a, b, *alsoHash)
    val r = w + x * c

    return GenericChaumPedersenProof(a, b, c, r)
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
 */
fun fakeGenericChaumPedersenProofOf(
    g: ElementModP,
    gx: ElementModP,
    h: ElementModP,
    hx: ElementModP,
    c: ElementModQ,
    seed: ElementModQ
): GenericChaumPedersenProof {
    compatibleContextOrFail(g, gx, h, hx, c, seed)
    val r = Nonces(seed, "generic-chaum-pedersen-proof")[0]
    val gr = g powP r
    val hr = h powP r
    val a = gr / (gx powP c)
    val b = hr / (hx powP c)
    return GenericChaumPedersenProof(a, b, c, r)
}