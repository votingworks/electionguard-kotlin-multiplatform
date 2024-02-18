@file:OptIn(ExperimentalCoroutinesApi::class)

package electionguard.core

import kotlin.test.assertFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.random.Random
import kotlin.random.nextUInt

/**
 * Kotest requires its properties to be executed as a suspending function. To make this all work,
 * we're using [kotlinx.coroutines.test.runTest] to do it. Note that this internal `runTest`
 * function requires that it be called *at most once per test method*. It's fine to put multiple
 * asserts or `forAll` calls or whatever else inside the `runTest` lambda body.
 */

fun runTest(f: suspend TestScope.() -> Unit) {
    // another benefit of having this wrapper code: we don't have to have the OptIn thing
    // at the top of every unit test file
    kotlinx.coroutines.test.runTest(EmptyCoroutineContext, 10_1000L, f)
}

/*
fun runTest(f: suspend TestScope.() -> Unit) {
    // another benefit of having this wrapper code: we don't have to have the OptIn thing
    // at the top of every unit test file
    kotlinx.coroutines.test.runTest { f() }
}

 */

/** Verifies that two byte arrays are different. */
fun assertContentNotEquals(a: ByteArray, b: ByteArray, message: String? = null) {
    assertFalse(a.contentEquals(b), message)
}

fun generateRangeChaumPedersenProofKnownNonce(
    context: GroupContext
): ChaumPedersenRangeProofKnownNonce {
    return ChaumPedersenRangeProofKnownNonce(
        listOf(generateGenericChaumPedersenProof(context)),
    )
}

fun generateGenericChaumPedersenProof(context: GroupContext): ChaumPedersenProof {
    return ChaumPedersenProof(generateElementModQ(context), generateElementModQ(context),)
}

fun generateSchnorrProof(context: GroupContext): SchnorrProof {
    return SchnorrProof(
        generatePublicKey(context),
        generateElementModQ(context),
        generateElementModQ(context),
    )
}

fun generateCiphertext(context: GroupContext): ElGamalCiphertext {
    return ElGamalCiphertext(generateElementModP(context), generateElementModP(context))
}

fun generateHashedCiphertext(context: GroupContext): HashedElGamalCiphertext {
    return HashedElGamalCiphertext(generateElementModP(context), "what".encodeToByteArray(), generateUInt256(context), 42)
}

fun generateElementModQ(context: GroupContext): ElementModQ {
    return context.uIntToElementModQ(Random.nextUInt(134217689.toUInt()))
}

fun generateUInt256(context: GroupContext): UInt256 {
    return generateElementModQ(context).toUInt256safe();
}

fun generateElementModP(context: GroupContext) = context.randomElementModP()

fun generatePublicKey(group: GroupContext): ElementModP =
    group.gPowP(group.randomElementModQ())
