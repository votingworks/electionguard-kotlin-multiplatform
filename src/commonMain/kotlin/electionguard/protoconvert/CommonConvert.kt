package electionguard.protoconvert

import electionguard.core.*
import electionguard.core.ElGamalPublicKey
import pbandk.ByteArr

fun electionguard.protogen.ElementModQ.importElementModQ(groupContext: GroupContext): ElementModQ {
    return groupContext.safeBinaryToElementModQ(this.value.array)
}

fun electionguard.protogen.ElementModP.importElementModP(groupContext: GroupContext): ElementModP {
    return groupContext.safeBinaryToElementModP(this.value.array)
}

fun electionguard.protogen.ElGamalCiphertext.importCiphertext(groupContext: GroupContext): ElGamalCiphertext {
    if (this.pad == null || this.data == null) {
        throw IllegalArgumentException("ElGamalCiphertext pad and value cannot be null")
    }
    return ElGamalCiphertext(
        this.pad.importElementModP(groupContext),
        this.data.importElementModP(groupContext)
    )
}

fun electionguard.protogen.ChaumPedersenProof.importChaumPedersenProof(groupContext: GroupContext): GenericChaumPedersenProof {
    if (this.pad == null || this.data == null || this.challenge == null || this.response == null) {
        throw IllegalArgumentException("ElGamalCiphertext pad and value cannot be null")
    }
    return GenericChaumPedersenProof(
        this.pad.importElementModP(groupContext),
        this.data.importElementModP(groupContext),
        this.challenge.importElementModQ(groupContext),
        this.response.importElementModQ(groupContext)
    )
}

fun electionguard.protogen.SchnorrProof.importSchnorrProof(groupContext: GroupContext): SchnorrProof {
    if (this.publicKey == null || this.commitment == null || this.challenge == null || this.response == null) {
        throw IllegalArgumentException("SchnorrProof fields cannot be null")
    }
    return SchnorrProof(
        this.publicKey.importElGamalPublicKey(groupContext),
        this.commitment.importElementModP(groupContext),
        this.challenge.importElementModQ(groupContext),
        this.response.importElementModQ(groupContext)
    )
}

fun electionguard.protogen.ElementModP.importElGamalPublicKey(groupContext: GroupContext) : ElGamalPublicKey {
    return ElGamalPublicKey(this.importElementModP(groupContext))
}

///////////////////////////////////////////////////////////////////////////////////////////////////////

fun ElementModQ.publishElementModQ(): electionguard.protogen.ElementModQ {
    return electionguard.protogen.ElementModQ(ByteArr(this.byteArray()))
}

fun ElementModP.publishElementModP(): electionguard.protogen.ElementModP {
    return electionguard.protogen.ElementModP(ByteArr(this.byteArray()))
}

fun ElGamalCiphertext.publishCiphertext(): electionguard.protogen.ElGamalCiphertext {
    return electionguard.protogen.ElGamalCiphertext(
        this.pad.publishElementModP(),
        this.data.publishElementModP()
    )
}

fun GenericChaumPedersenProof.publishChaumPedersenProof(): electionguard.protogen.ChaumPedersenProof {
    return electionguard.protogen.ChaumPedersenProof(
        this.a.publishElementModP(),
        this.b.publishElementModP(),
        this.c.publishElementModQ(),
        this.r.publishElementModQ()
    )
}

fun SchnorrProof.publishSchnorrProof(): electionguard.protogen.SchnorrProof {
    return electionguard.protogen.SchnorrProof(
        this.publicKey.publishElGamalPublicKey(),
        this.commitment.publishElementModP(),
        this.challenge.publishElementModQ(),
        this.response.publishElementModQ()
    )
}

fun ElGamalPublicKey.publishElGamalPublicKey() : electionguard.protogen.ElementModP {
    return this.key.publishElementModP()
}