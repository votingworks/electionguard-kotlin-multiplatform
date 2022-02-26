package electionguard.protoconvert

import electionguard.core.*
import electionguard.core.ElGamalPublicKey
import pbandk.ByteArr

fun convertElementModQ(modQ: electionguard.protogen.ElementModQ, groupContext: GroupContext): ElementModQ {
    return groupContext.safeBinaryToElementModQ(modQ.value.array)
}

fun convertElementModP(modP: electionguard.protogen.ElementModP, groupContext: GroupContext): ElementModP {
    return groupContext.safeBinaryToElementModP(modP.value.array)
}

fun convertCiphertext(
    ciphertext: electionguard.protogen.ElGamalCiphertext,
    groupContext: GroupContext
): ElGamalCiphertext {
    if (ciphertext.pad == null || ciphertext.data == null) {
        throw IllegalArgumentException("ElGamalCiphertext pad and value cannot be null")
    }
    return ElGamalCiphertext(
        convertElementModP(ciphertext.pad, groupContext),
        convertElementModP(ciphertext.data, groupContext)
    )
}

fun convertChaumPedersenProof(proof: electionguard.protogen.ChaumPedersenProof,
                              groupContext: GroupContext): GenericChaumPedersenProof {
    if (proof.pad == null || proof.data == null || proof.challenge == null || proof.response == null) {
        throw IllegalArgumentException("ElGamalCiphertext pad and value cannot be null")
    }
    return GenericChaumPedersenProof(
        convertElementModP(proof.pad, groupContext),
        convertElementModP(proof.data, groupContext),
        convertElementModQ(proof.challenge, groupContext),
        convertElementModQ(proof.response, groupContext)
    )
}

fun convertSchnorrProof(proof: electionguard.protogen.SchnorrProof,
                        groupContext: GroupContext): SchnorrProof {
    if (proof.publicKey == null || proof.commitment == null || proof.challenge == null || proof.response == null) {
        throw IllegalArgumentException("SchnorrProof fields cannot be null")
    }
    return SchnorrProof(
        convertElGamalPublicKey(proof.publicKey, groupContext),
        convertElementModP(proof.commitment, groupContext),
        convertElementModQ(proof.challenge, groupContext),
        convertElementModQ(proof.response, groupContext)
    )
}

fun convertElGamalPublicKey(publicKey: electionguard.protogen.ElementModP,
                            groupContext: GroupContext) : ElGamalPublicKey {
    return ElGamalPublicKey(convertElementModP(publicKey, groupContext))
}

///////////////////////////////////////////////////////////////////////////////////////////////////////

fun convertElementModQ(modQ: ElementModQ): electionguard.protogen.ElementModQ {
    return electionguard.protogen.ElementModQ(ByteArr(modQ.byteArray()))
}

fun convertElementModP(modP: ElementModP): electionguard.protogen.ElementModP {
    return electionguard.protogen.ElementModP(ByteArr(modP.byteArray()))
}

fun convertCiphertext(
    ciphertext: ElGamalCiphertext): electionguard.protogen.ElGamalCiphertext {
    return electionguard.protogen.ElGamalCiphertext(
        convertElementModP(ciphertext.pad),
        convertElementModP(ciphertext.data)
    )
}

fun convertChaumPedersenProof(proof: GenericChaumPedersenProof): electionguard.protogen.ChaumPedersenProof {
    return electionguard.protogen.ChaumPedersenProof(
        convertElementModP(proof.a),
        convertElementModP(proof.b),
        convertElementModQ(proof.c),
        convertElementModQ(proof.r)
    )
}

fun convertSchnorrProof(proof: SchnorrProof): electionguard.protogen.SchnorrProof {
    return electionguard.protogen.SchnorrProof(
        convertElGamalPublicKey(proof.publicKey),
        convertElementModP(proof.commitment),
        convertElementModQ(proof.challenge),
        convertElementModQ(proof.response)
    )
}

fun convertElGamalPublicKey(publicKey: ElGamalPublicKey) : electionguard.protogen.ElementModP {
    return convertElementModP(publicKey.key)
}