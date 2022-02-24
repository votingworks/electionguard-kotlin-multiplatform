package electionguard.protoconvert

import electionguard.core.*
import kotlin.random.Random
import kotlin.random.nextUInt

fun generateConstantChaumPedersenProofKnownNonce(context: GroupContext): ConstantChaumPedersenProofKnownNonce {
    return ConstantChaumPedersenProofKnownNonce(
        generateGenericChaumPedersenProof(context),
        Random.nextInt(),
    )
}

fun generateDisjunctiveChaumPedersenProofKnownNonce(context: GroupContext): DisjunctiveChaumPedersenProofKnownNonce {
    return DisjunctiveChaumPedersenProofKnownNonce(
        generateGenericChaumPedersenProof(context),
        generateGenericChaumPedersenProof(context),
        generateElementModQ(context),
    )
}

fun generateGenericChaumPedersenProof(context: GroupContext): GenericChaumPedersenProof {
    return GenericChaumPedersenProof(
        generateElementModP(context),
        generateElementModP(context),
        generateElementModQ(context),
        generateElementModQ(context),
    )
}

//     val publicKey: ElGamalPublicKey,
//    val commitment: ElementModP,
//    val challenge: ElementModQ,
//    val response: ElementModQ
fun generateSchnorrProof(context: GroupContext): SchnorrProof {
    return SchnorrProof(
        ElGamalPublicKey(generateElementModP(context)),
        generateElementModP(context),
        generateElementModQ(context),
        generateElementModQ(context),
    )
}

fun generateCiphertext(context: GroupContext): ElGamalCiphertext {
    return ElGamalCiphertext(generateElementModP(context), generateElementModP(context))
}

fun generateElementModQ(context: GroupContext): ElementModQ {
    return context.uIntToElementModQ(Random.nextUInt())
}

fun generateElementModP(context: GroupContext): ElementModP {
    return context.uIntToElementModP(Random.nextUInt())
}