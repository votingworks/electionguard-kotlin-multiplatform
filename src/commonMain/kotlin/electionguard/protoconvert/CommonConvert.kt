package electionguard.protoconvert

import electionguard.core.*
import electionguard.core.ElGamalPublicKey
import pbandk.ByteArr

fun GroupContext.importElementModQ(modQ: electionguard.protogen.ElementModQ?): ElementModQ? =
    if (modQ == null) null else this.binaryToElementModQ(modQ.value.array)

fun importUInt256(modQ: electionguard.protogen.UInt256?): UInt256? =
    modQ?.value?.array?.toUInt256()

fun GroupContext.importElementModP(modP: electionguard.protogen.ElementModP?): ElementModP? =
    if (modP == null) null else this.binaryToElementModP(modP.value.array)

fun GroupContext.importCiphertext(
    ciphertext: electionguard.protogen.ElGamalCiphertext?,
): ElGamalCiphertext? {
    if (ciphertext == null) return null
    val pad = this.importElementModP(ciphertext.pad)
    val data = this.importElementModP(ciphertext.data)

    return if (pad == null || data == null) null else ElGamalCiphertext(pad, data)
}

fun GroupContext.importChaumPedersenProof(
    proof: electionguard.protogen.GenericChaumPedersenProof?,
): GenericChaumPedersenProof? {
    if (proof == null) return null
    val challenge = this.importElementModQ(proof.challenge)
    val response = this.importElementModQ(proof.response)

    return if (challenge == null || response == null) null else GenericChaumPedersenProof(challenge, response)
}

fun GroupContext.importHashedCiphertext(
    ciphertext: electionguard.protogen.HashedElGamalCiphertext?,
): HashedElGamalCiphertext? {
    if (ciphertext == null) return null
    val c0 = this.importElementModP(ciphertext.c0)
    val c2 = importUInt256(ciphertext.c2)

    return if (c0 == null || c2 == null) null else HashedElGamalCiphertext(c0, ciphertext.c1.array, c2, ciphertext.numBytes)
}

fun GroupContext.importSchnorrProof(proof: electionguard.protogen.SchnorrProof?): SchnorrProof? {
    if (proof == null) return null
    val challenge = this.importElementModQ(proof.challenge)
    val response = this.importElementModQ(proof.response)

    return if (challenge == null || response == null) null else SchnorrProof(challenge, response)
}

fun GroupContext.importElGamalPublicKey(
    publicKey: electionguard.protogen.ElementModP?,
) : ElGamalPublicKey? {
    if (publicKey == null) return null
    val key = this.importElementModP(publicKey)

    return if (key == null) null else ElGamalPublicKey(key)
}

///////////////////////////////////////////////////////////////////////////////////////////////////////

fun ElementModQ.publishElementModQ(): electionguard.protogen.ElementModQ {
    return electionguard.protogen.ElementModQ(ByteArr(this.byteArray()))
}

fun UInt256.publishUInt256(): electionguard.protogen.UInt256 {
    return electionguard.protogen.UInt256(ByteArr(this.bytes))
}

fun ElementModP.publishElementModP(): electionguard.protogen.ElementModP {
    return electionguard.protogen.ElementModP(ByteArr(this.byteArray()))
}

fun ElGamalCiphertext.publishCiphertext(): electionguard.protogen.ElGamalCiphertext {
    return electionguard.protogen
        .ElGamalCiphertext(this.pad.publishElementModP(), this.data.publishElementModP())
}

fun GenericChaumPedersenProof.publishChaumPedersenProof():
    electionguard.protogen.GenericChaumPedersenProof {
        return electionguard.protogen
            .GenericChaumPedersenProof(
                null, // 1.0 0nly
                null, // 1.0 0nly
                this.c.publishElementModQ(),
                this.r.publishElementModQ(),
            )
    }

fun HashedElGamalCiphertext.publishHashedCiphertext() :
    electionguard.protogen.HashedElGamalCiphertext {
    return electionguard.protogen.HashedElGamalCiphertext(
        this.c0.publishElementModP(),
        ByteArr(this.c1),
        this.c2.publishUInt256(),
        this.numBytes,
    )
}

fun SchnorrProof.publishSchnorrProof(): electionguard.protogen.SchnorrProof {
    return electionguard.protogen
        .SchnorrProof(
            null,   // 1.0 0nly
            null, // 1.0 0nly
            this.challenge.publishElementModQ(),
            this.response.publishElementModQ()
        )
}

fun ElGamalPublicKey.publishElGamalPublicKey() : electionguard.protogen.ElementModP {
    return this.key.publishElementModP()
}