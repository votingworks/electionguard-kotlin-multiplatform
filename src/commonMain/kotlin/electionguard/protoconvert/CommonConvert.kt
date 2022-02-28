package electionguard.protoconvert

import electionguard.core.*
import electionguard.core.ElGamalPublicKey
import mu.KotlinLogging
import pbandk.ByteArr
private val logger = KotlinLogging.logger("CommonConvert")

/**
 * Converts a deserialized ElementModQ to the internal representation, returning `null` if the input
 * was missing, out of bounds, or otherwise malformed. `null` is accepted as an input, for
 * convenience. If `null` is passed as input, `null` is returned.
 */
fun convertElementModQ(
    modQ: electionguard.protogen.ElementModQ?,
    groupContext: GroupContext
): ElementModQ? = if (modQ == null) null else groupContext.binaryToElementModQ(modQ.value.array)

/**
 * Converts a deserialized ElementModP to the internal representation, returning `null` if the input
 * was missing, out of bounds, or otherwise malformed. `null` is accepted as an input, for
 * convenience. If `null` is passed as input, `null` is returned.
 */
fun convertElementModP(
    modP: electionguard.protogen.ElementModP?,
    groupContext: GroupContext
): ElementModP? = if (modP == null) null else groupContext.binaryToElementModP(modP.value.array)

fun convertCiphertext(
    ciphertext: electionguard.protogen.ElGamalCiphertext?,
    groupContext: GroupContext
): ElGamalCiphertext? {
    if (ciphertext == null) return null

    val pad = convertElementModP(ciphertext.pad, groupContext)
    val data = convertElementModP(ciphertext.data, groupContext)

    if (pad == null || data == null) {
        logger.error { "ElGamalCiphertext pad or value was malformed or out of bounds" }
        return null
    }

    return ElGamalCiphertext(pad, data);
}

fun convertChaumPedersenProof(
    proof: electionguard.protogen.ChaumPedersenProof?,
    groupContext: GroupContext
): GenericChaumPedersenProof? {
    // TODO: this should probably be a ConstantChaumPedersenProofKnownSecretKey, which needs to know
    //   what the constant actually is. That should be nearby in the serialized data.

    if (proof == null) return null

    val pad = convertElementModP(proof.pad, groupContext)
    val data = convertElementModP(proof.data, groupContext)
    val challenge = convertElementModQ(proof.challenge, groupContext)
    val response = convertElementModQ(proof.response, groupContext)

    if (pad == null || data == null || challenge == null || response == null) {
        logger.error { "one or more ChaumPedersenProof inputs was malformed or out of bounds" }
        return null
    }

    return GenericChaumPedersenProof(pad, data, challenge, response)
}

fun convertSchnorrProof(
    proof: electionguard.protogen.SchnorrProof?,
    groupContext: GroupContext
): SchnorrProof? {

    if (proof == null) return null

    val publicKey = convertElGamalPublicKey(proof.publicKey, groupContext)
    val commitment = convertElementModP(proof.commitment, groupContext)
    val challenge = convertElementModQ(proof.challenge, groupContext)
    val response = convertElementModQ(proof.response, groupContext)

    if (publicKey == null || commitment == null || challenge == null || response == null) {
        logger.error { "one or more SchnorrProof inputs was malformed or out of bounds" }
        return null
    }

    return SchnorrProof(publicKey, commitment, challenge, response)
}

fun convertElGamalPublicKey(
    publicKey: electionguard.protogen.ElementModP?,
    groupContext: GroupContext
) : ElGamalPublicKey? {
    val key = convertElementModP(publicKey, groupContext)
    if (key == null) {
        logger.error { "ElGamalPublicKey was malformed or out of bounds" }
        return null
    }
    return ElGamalPublicKey(key)
}

///////////////////////////////////////////////////////////////////////////////////////////////////////

fun convertElementModQ(modQ: ElementModQ): electionguard.protogen.ElementModQ {
    return electionguard.protogen.ElementModQ(ByteArr(modQ.byteArray()))
}

fun convertElementModP(modP: ElementModP): electionguard.protogen.ElementModP {
    return electionguard.protogen.ElementModP(ByteArr(modP.byteArray()))
}

fun convertCiphertext(ciphertext: ElGamalCiphertext): electionguard.protogen.ElGamalCiphertext {
    return electionguard.protogen
        .ElGamalCiphertext(convertElementModP(ciphertext.pad), convertElementModP(ciphertext.data))
}

fun convertChaumPedersenProof(
    proof: GenericChaumPedersenProof
): electionguard.protogen.ChaumPedersenProof {
    return electionguard.protogen
        .ChaumPedersenProof(
            convertElementModP(proof.a),
            convertElementModP(proof.b),
            convertElementModQ(proof.c),
            convertElementModQ(proof.r)
        )
}

fun convertSchnorrProof(proof: SchnorrProof): electionguard.protogen.SchnorrProof {
    return electionguard.protogen
        .SchnorrProof(
            convertElGamalPublicKey(proof.publicKey),
            convertElementModP(proof.commitment),
            convertElementModQ(proof.challenge),
            convertElementModQ(proof.response)
        )
}

fun convertElGamalPublicKey(publicKey: ElGamalPublicKey) : electionguard.protogen.ElementModP {
    return convertElementModP(publicKey.key)
}