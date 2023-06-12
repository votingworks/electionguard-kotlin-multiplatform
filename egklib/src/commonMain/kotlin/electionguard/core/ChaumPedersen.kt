package electionguard.core

import com.github.michaelbull.result.*
import kotlin.collections.fold

// Everything needed for version 2 is in ChaumPedersen2.kt

/**
 * Given a [ChaumPedersenProof], computes the `a` and `b` values that are needed for proofs
 * and such, but are removed for serialization.
 */
fun ChaumPedersenProof.expand(
    g: ElementModP,
    gx: ElementModP,
    h: ElementModP,
    hx: ElementModP,
): ExpandedChaumPedersenProof {
    val gr = g powP r // g^r = g^(w - xc)
    val hr = h powP r // h^r = h^(w - xc)
    val a = gr * (gx powP c) // cancelling out the xc, getting g^w
    val b = hr * (hx powP c) // cancelling out the xc, getting h^w
    return ExpandedChaumPedersenProof(a, b, c, r)
}

/**
 * Produces a proof that a given ElGamal encryption corresponds to a value between zero and a
 * given limit (inclusive), given that the prover to know the nonce (the r behind the g^r).
 *
 * @param plaintext The actual plaintext constant value used to make the ElGamal ciphertext (ℓ in the spec)
 * @param limit The maximum possible value for the plaintext (inclusive), (L in the spec)
 * @param nonce The aggregate nonce used creating the ElGamal ciphertext (r in the spec)
 * @param publicKey The ElGamal public key for the election
 * @param seed Used to generate other random values here
 * @param qbar The election extended base hash (Q')
 * @param overrideErrorChecks Allows the creation of invalid proofs
 */
fun ElGamalCiphertext.rangeChaumPedersenProofKnownNonce(
    plaintext: Int,
    limit: Int,
    aggNonce: ElementModQ,
    publicKey: ElGamalPublicKey,
    qbar: ElementModQ,
    overrideErrorChecks: Boolean = false
): ChaumPedersenRangeProofKnownNonce {
    if (!overrideErrorChecks && plaintext < 0) {
        throw ArithmeticException("negative plaintexts not supported")
    }
    if (!overrideErrorChecks && limit < 0) {
        throw ArithmeticException("negative limits not supported")
    }
    if (!overrideErrorChecks && limit < plaintext) {
        throw ArithmeticException("limit must be at least as big as the plaintext")
    }

    val (alpha, beta) = this
    val context = compatibleContextOrFail(pad, aggNonce, publicKey.key, qbar, alpha, beta)

    // get some random nonces
     val uList = Nonces(aggNonce, "range-chaum-pedersen-proof").take(limit + 1)
    // Randomly chosen c-values; note that c[plaintext] (c_l in the spec) should
    // not be taken from this list; that's going to be computed later on.
    val cList = Nonces(aggNonce, "range-chaum-pedersen-proof-constants").take(limit + 1)

    val aList = uList.map { u -> context.gPowP(u) }
    val bList = uList.mapIndexed { j, u ->
        if (j == plaintext) {
            // Spec, page 22, equation 48.
            // We're not using cList for this specific b value.
            publicKey powP u
        } else {
            // Spec, page 22, equation 49.

            // We can't convert a negative number to an ElementModQ,
            // so we instead use the unaryMinus operator.
            val plaintextMinusIndex = if (plaintext >= j)
                (plaintext - j).toElementModQ(context)
            else
                -((j - plaintext).toElementModQ(context))

            publicKey powP (plaintextMinusIndex * cList[j] + u)
        }
    }

    // (a1, a2, a3) x (b1, b2, b3) ==> (a1, b1, a2, b2, a3, b3, ...)
    val hashMe = aList.zip(bList).flatMap { listOf(it.first, it.second) }

    // Spec 1.52, equation 47; we need to have this very specific ordering of inputs for computing c.
    val c = hashElements(qbar, publicKey, alpha, beta, *(hashMe.toTypedArray())).toElementModQ(context)

    // Spec 1.52, equation 48. (c_l)
    val cl = c -
            cList.filterIndexed { j, _ -> j != plaintext }
                .fold(context.ZERO_MOD_Q) { a, b -> a + b }

    val cListFinal = cList.mapIndexed { j, cj -> if (j == plaintext) cl else cj }

    val vList = uList.zip(cList).mapIndexed { j, (uj, cj) ->
        val cjActual = if (j == plaintext) cl else cj

        // Spec 1.9, page 31, equation 57 (v_j)
        uj - cjActual * aggNonce
    }

    return ChaumPedersenRangeProofKnownNonce(
        cListFinal.zip(vList).map{ (cj, vj) ->
            ChaumPedersenProof(cj, vj)
        },
    )
}

/**
 * Validates a range proof against an ElGamal ciphertext for the range [0, limit], inclusive.
 * Validates 4.A, 4.B, 4.C, 5.B, 5.C
 *
 * @param ciphertext An ElGamal ciphertext
 * @param publicKey The public key of the election
 * @param qbar The election extended base hash (Q')
 * @param limit The maximum possible value for the plaintext (inclusive)
 * @return true if the proof is valid, else an error message
 */
fun ChaumPedersenRangeProofKnownNonce.validate(
    ciphertext: ElGamalCiphertext,
    publicKey: ElGamalPublicKey,
    qbar: ElementModQ,
    limit: Int
): Result<Boolean, String> {
    val context = compatibleContextOrFail(this.proofs[0].c, ciphertext.pad, publicKey.key, qbar)
    val results = mutableListOf<Result<Boolean, String>>()

    if (limit + 1 != proofs.size) {
        return Err("    RangeProof expected ${limit + 1} proofs, only found ${proofs.size}")
    }

    val (alpha, beta) = ciphertext
    results.add(
        if (alpha.isValidResidue() && beta.isValidResidue()) Ok(true) else
            Err("    4.A,5.A invalid residue: alpha = ${alpha.inBounds()} beta = ${beta.inBounds()}")
    )

    val expandedProofs = proofs.mapIndexed { j, proof ->
        // recomputes all the a and b values
        val (cj, vj) = proof
        results.add(
            if (cj.inBounds() && vj.inBounds()) Ok(true) else
                Err("    4.B,5.B c = ${cj.inBounds()} v = ${vj.inBounds()} idx=$j")
        )

        val wj = (vj - j.toElementModQ(context) * cj)
        ExpandedChaumPedersenProof(
            a = context.gPowP(vj) * (alpha powP cj), // 4.1, 4.2, 5.3
            b = (publicKey powP wj) * (beta powP cj), // 4.3, 4.4, 5.4
            c = cj,
            r = vj)

        // TODO: figure out how to do this with the proof.expand() method.
        //   The way all the c-values fit together in the spec is different
        //   enough that our "generic" expansion code isn't quite generic
        //   enough that we can solve for the proper arguments to
        //   pass the method as-is.
    }

    // sum of the proof.c
    val abList = expandedProofs.flatMap { listOf(it.a, it.b) }.toTypedArray()
    val c = hashElements(qbar, publicKey, alpha, beta, *abList).toElementModQ(context) // 4.5, 5.5
    val cSum = this.proofs.fold(context.ZERO_MOD_Q) { a, b -> a + b.c }
    results.add(
        if (cSum == c) Ok(true) else Err("    4.C,5.C challenge sum is invalid")
    )

    return results.merge()
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
 * @param hashHeader Optional additional values to include in the hash challenge computation hash
 * @param hashFooter Optional additional values to include in the hash challenge computation hash
 * @param checkC If false, the challenge constant is not verified. (default: true)
 * @return true if the proof is valid, else an error message
 */
internal fun ChaumPedersenProof.validate(
    g: ElementModP,
    gx: ElementModP,
    h: ElementModP,
    hx: ElementModP,
    hashHeader: Array<Element>,
    hashFooter: Array<Element> = emptyArray(),
    checkC: Boolean = true,
): Result<Boolean, String> {
    return expand(g, gx, h, hx).validate(g, gx, h, hx, hashHeader, hashFooter, checkC)
}

internal fun ExpandedChaumPedersenProof.validate(
    g: ElementModP,
    gx: ElementModP,
    h: ElementModP,
    hx: ElementModP,
    hashHeader: Array<Element>,
    hashFooter: Array<Element> = emptyArray(),
    checkC: Boolean = true
): Result<Boolean, String> {
    val context = compatibleContextOrFail(c, g, gx, h, hx, *hashHeader, *hashFooter)
    val errors = mutableListOf<Result<Boolean, String>>()

    val inBoundsG = g.isValidResidue()
    val inBoundsGx = gx.isValidResidue()
    val inBoundsH = h.isValidResidue()
    val inBoundsHx = hx.isValidResidue()

    if (!(inBoundsG && inBoundsGx && inBoundsH && inBoundsHx)) {
        errors.add(Err("  Invalid residual: " +
                mapOf(
                    "inBoundsG" to inBoundsG,
                    "inBoundsGx" to inBoundsGx,
                    "inBoundsH" to inBoundsH,
                    "inBoundsHx" to inBoundsHx,
                ).toString()))
    }

    val hashGood = !checkC || c == hashElements(*hashHeader, a, b, *hashFooter).toElementModQ(context)
    if (!hashGood) {
        errors.add(Err("  Invalid challenge "))
    }

    return errors.merge()
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
 * @param hashHeader Optional additional values to include in the start of the challenge computation hash.
 * @param hashFooter Optional additional values to include in the end of the challenge computation hash.
 */
fun genericChaumPedersenProofOf(
    g: ElementModP,
    h: ElementModP,
    x: ElementModQ, // secret
    seed: ElementModQ,
    hashHeader: Array<Element>,
    hashFooter: Array<Element> = emptyArray(),
): ChaumPedersenProof {
    val context = compatibleContextOrFail(g, h, x, seed, *hashHeader, *hashFooter)

    // The proof generates a random value w ∈ Z q , computes the commitments (a , b) = (g^w , A^w),
    // obtains the challenge value as c = H( Q̄, A, B, a , b, M)
    // and the response r = (w - c * s) mod q.
    // The proof is (a, b, c, r)

    val w = Nonces(seed, "generic-chaum-pedersen-proof")[0]
    val a = g powP w
    val b = h powP w

    val c = hashElements(*hashHeader, a, b, *hashFooter).toElementModQ(context)
    val r = w - x * c

    return ChaumPedersenProof(c, r)
}

/**
 * Produces a generic "fake" Chaum-Pedersen proof that two tuples share an exponent, i.e., that for
 * (g, g^x) and (h, h^x), it's the same value of x, but without revealing x. Unlike the regular
 * Chaum-Pedersen proof, this version allows the challenge `c` to be specified, which allows
 * everything to be faked. See the [ChaumPedersenProof.validate] method on the resulting proof
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
fun fakeGenericChaumPedersenProofOf(c: ElementModQ, seed: ElementModQ): ChaumPedersenProof {
    compatibleContextOrFail(c, seed)
    val r = Nonces(seed, "generic-chaum-pedersen-proof")[0]
    return ChaumPedersenProof(c, r)
}