package electionguard.core

import com.github.michaelbull.result.*
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.core.Base16.toHex
import kotlin.collections.fold

/**
 * Disjunctive proof that the ciphertext is between zero and a maximum
 * value, inclusive. Note that the size of the proof is proportional to
 * the maximum value. Example: a proof that a ciphertext is in [0, 5]
 * will have six internal proof components.
 */
data class ChaumPedersenRangeProofKnownNonce(
    val proofs: List<ChaumPedersenProof>, // L + 1
)

/**
 * General-purpose Chaum-Pedersen proof object, for demonstrating that the prover knows the exponent `x`
 * for two tuples `(g, g^x)` and `(h, h^x)`, without revealing anything about `x`.
 * (See [Chaum-Pedersen 1992](https://link.springer.com/chapter/10.1007/3-540-48071-4_7))
 *
 * @param c challenge
 * @param r response
 */
data class ChaumPedersenProof(val c: ElementModQ, val r: ElementModQ)

/**
 * Expanded form of the [ChaumPedersenProof], with the `a` and `b` values recomputed. This
 * should not be serialized.
 */
data class ExpandedChaumPedersenProof(
    val a: ElementModP,
    val b: ElementModP,
    val c: ElementModQ,
    val r: ElementModQ,
)

// spec 2.0.0, section 3.3.5
// Proves that (α, β) is an encryption of an integer in the range 0, 1, . . . , L.
// (Requires knowledge of encryption nonce ξ for which (α, β) is an encryption of ℓ.)
fun ElGamalCiphertext.makeChaumPedersen(
    vote: Int, // ℓ
    limit: Int,     // L
    nonce: ElementModQ, // encryption nonce ξ for which (α, β) is an encryption of ℓ.
    publicKey: ElGamalPublicKey, // K
    extendedBaseHash: UInt256, // He
    overrideErrorChecks: Boolean = false
): ChaumPedersenRangeProofKnownNonce {
    if (!overrideErrorChecks && vote < 0) {
        throw ArithmeticException("negative plaintexts not supported")
    }
    if (!overrideErrorChecks && limit < 0) {
        throw ArithmeticException("negative limits not supported")
    }
    if (!overrideErrorChecks && limit < vote) {
        throw ArithmeticException("limit must be at least as big as the plaintext")
    }

    return this.makeChaumPedersenWithNonces(
        vote,
        limit,
        nonce,
        publicKey,
        extendedBaseHash,
        Nonces(nonce, "range-chaum-pedersen-proof").take(limit + 1),
        Nonces(nonce, "range-chaum-pedersen-proof-constants").take(limit + 1)
    )
}

fun ElGamalCiphertext.makeChaumPedersenWithNonces(
    vote: Int, // ℓ
    limit: Int,     // L
    nonce: ElementModQ, // encryption nonce ξ for which (α, β) is an encryption of ℓ.
    publicKey: ElGamalPublicKey, // K
    extendedBaseHash: UInt256, // He
    randomUj: List<ElementModQ>, // size == L + 1
    randomCj: List<ElementModQ>, // size == L + 1
): ChaumPedersenRangeProofKnownNonce {
    val (alpha, beta) = this
    val group = compatibleContextOrFail(pad, nonce, publicKey.key, alpha, beta)

    // (aℓ , bℓ ) = (g^uℓ mod p, K^uℓ mod p), for j = ℓ (eq 44)
    // (aj , bj ) = (g^uj mod p, K^tj mod p), where tj = (uj +(ℓ−j) * cj ), for j != ℓ (eq 45)
    val aList = randomUj.map { u -> group.gPowP(u) }
    val bList = randomUj.mapIndexed { j, u ->
        if (j == vote) {
            //  j = ℓ
            publicKey powP u
        } else {
            //  j != ℓ
            // We can't convert a negative number to an ElementModQ,
            // so we instead use the unaryMinus operator.
            val plaintextMinusIndex = if (vote >= j)
                (vote - j).toElementModQ(group)
            else
                -((j - vote).toElementModQ(group))

            publicKey powP (plaintextMinusIndex * randomCj[j] + u) // K^tj, where tj = (uj +(ℓ−j) * cj )
        }
    }

    // (a1, a2, a3) x (b1, b2, b3) ==> (a1, b1, a2, b2, a3, b3, ...)
    val abList: List<ElementModP> = aList.zip(bList).flatMap { listOf(it.first, it.second) }

    // c = H(HE ; 0x21, K, α, β, a0 , b0 , a1 , b1 , . . . , aR , bR ) ; eq 46
    val c = hashFunction(extendedBaseHash.bytes, 0x21.toByte(), publicKey.key, alpha, beta, abList).toElementModQ(group)

    // cl = (c − (c0 + · · · + cℓ−1 + cℓ+1 + · · · + cR )) mod q ; eq 47
    val cl = c -
            randomCj.filterIndexed { j, _ -> j != vote }
                .fold(group.ZERO_MOD_Q) { a, b -> a + b }

    // responses are computed for all 0 ≤ j ≤ L as vj = (uj − cj * ξ) mod q, eq 48
    val vList = randomUj.zip(randomCj).mapIndexed { j, (uj, cj) ->
        val cjActual = if (j == vote) cl else cj
        uj - cjActual * nonce
    }

    // substitute cl into the random challenges when j = l
    val cListFinal = randomCj.mapIndexed { j, cj -> if (j == vote) cl else cj }
    // turn the challenges and responses into a list of GenericChaumPedersenProof(cj, vj)
    val cpgList : List<ChaumPedersenProof> = cListFinal.zip(vList).map{ (cj, vj) ->
        ChaumPedersenProof(cj, vj)
    }
    return ChaumPedersenRangeProofKnownNonce(cpgList)
}

// Verification 5 (Well-formedness of selection encryptions) TODO check complete
// Verification 6 (Adherence to vote limits) TODO check complete
fun ChaumPedersenRangeProofKnownNonce.validate2(
    ciphertext: ElGamalCiphertext,
    publicKey: ElGamalPublicKey, // K
    extendedBaseHash: UInt256, // He
    limit: Int
): Result<Boolean, String> {
    val group = compatibleContextOrFail(this.proofs[0].c, ciphertext.pad, publicKey.key)
    val results = mutableListOf<Result<Boolean, String>>()

    if (limit + 1 != proofs.size) {
        return Err("    RangeProof expected ${limit + 1} proofs, but found ${proofs.size}")
    }

    val (alpha, beta) = ciphertext
    results.add(
        if (alpha.isValidResidue() && beta.isValidResidue()) Ok(true) else
            Err("    5.A,6.A invalid residue: alpha = ${alpha.inBounds()} beta = ${beta.inBounds()}")
    )

    val expandedProofs = proofs.mapIndexed { j, proof ->
        // recomputes all the a and b values
        val (cj, vj) = proof
        results.add(
            if (cj.inBounds() && vj.inBounds()) Ok(true) else
                Err("    5.B,6.B c = ${cj.inBounds()} v = ${vj.inBounds()} idx=$j")
        )

        val wj = (vj - j.toElementModQ(group) * cj)
        ExpandedChaumPedersenProof(
            a = group.gPowP(vj) * (alpha powP cj), // 4.3
            b = (publicKey powP wj) * (beta powP cj), // 4.4
            c = cj,
            r = vj)
    }

    // sum of the proof.c
    val abList = expandedProofs.flatMap { listOf(it.a, it.b) }

    // c = H(HE ; 0x21, K, α, β, a0 , b0 , a1 , b1 , . . . , aR , bR ) ; eq 5.3
    val c = hashFunction(extendedBaseHash.bytes, 0x21.toByte(), publicKey.key, alpha, beta, abList).toElementModQ(group)

    // The equation c = (c0 + c1 + · · · + cR ) mod q is satisfied ; eq 5.D
    val cSum = this.proofs.fold(group.ZERO_MOD_Q) { a, b -> a + b.c }
    results.add(
        if (cSum == c) Ok(true) else Err("    5.D,6.D challenge sum is invalid")
    )

    return results.merge()
}

// Verification 9 (Correctness of tally decryptions)
fun ChaumPedersenProof.validate2(
    publicKey: ElementModP, // K
    extendedBaseHash: UInt256, // He
    bOverM: ElementModP,
    encryptedVote: ElGamalCiphertext,
): Boolean {
    val group = compatibleContextOrFail(publicKey, encryptedVote.pad, encryptedVote.data, bOverM)
    val M: ElementModP = encryptedVote.data / bOverM // eq 9.1
    val a = group.gPowP(this.r) * (publicKey powP this.c) // 9.2
    val b = (encryptedVote.pad powP this.r) * (M powP this.c) // 9.3

    // The challenge value c satisfies c = H(HE ; 0x30, K, A, B, a, b, M ). eq 9.B
    val challenge = hashFunction(extendedBaseHash.bytes, 0x30.toByte(), publicKey, encryptedVote.pad, encryptedVote.data, a, b, M)
    return (challenge.toElementModQ(group) == this.c)
}

// Verification 11 (Correctness of decryptions of contest data)
fun ChaumPedersenProof.validate2(
    publicKey: ElementModP, // K
    extendedBaseHash: UInt256, // He
    beta: ElementModP,
    hashedCiphertext: HashedElGamalCiphertext,
): Boolean {
    val group = compatibleContextOrFail(publicKey, hashedCiphertext.c0, beta)
    val a = group.gPowP(this.r) * (publicKey powP this.c) // 11.1
    val b = (hashedCiphertext.c0 powP this.r) * (beta powP this.c) // 11.2

    // The challenge value c satisfies c = H(HE ; 0x31, K, C0 , C1 , C2 , a, b, β) ; (11.B)
    val challenge = hashFunction(extendedBaseHash.bytes, 0x31.toByte(), publicKey,
        hashedCiphertext.c0,
        hashedCiphertext.c1.toHex(),
        hashedCiphertext.c2,
        a, b, beta)
    return (challenge.toElementModQ(group) == this.c)
}
