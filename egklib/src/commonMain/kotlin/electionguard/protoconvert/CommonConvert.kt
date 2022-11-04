package electionguard.protoconvert

import electionguard.core.*
import pbandk.ByteArr

fun GroupContext.importElementModQ(modQ: electionguard.protogen.ElementModQ?): ElementModQ? =
    modQ?.let { this.binaryToElementModQ(modQ.value.array) }

fun importUInt256(modQ: electionguard.protogen.UInt256?): UInt256? =
    modQ?.value?.array?.toUInt256()

fun GroupContext.importElementModP(modP: electionguard.protogen.ElementModP?): ElementModP? =
    modP?.let { this.binaryToElementModP(modP.value.array) }

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
    return if (c0 == null || c2 == null) null else HashedElGamalCiphertext(
        c0,
        ciphertext.c1.array,
        c2,
        ciphertext.numBytes
    )
}

fun GroupContext.importSchnorrProof(proof: electionguard.protogen.SchnorrProof?): SchnorrProof? {
    if (proof == null) return null
    val publicKey = this.importElementModP(proof.publicKey)
    val challenge = this.importElementModQ(proof.challenge)
    val response = this.importElementModQ(proof.response)

    return if (publicKey == null || challenge == null || response == null) null
    else SchnorrProof(publicKey, challenge, response)
}

///////////////////////////////////////////////////////////////////////////////////////////////////////

fun ElementModQ.publishElementModQ() = electionguard.protogen.ElementModQ(ByteArr(this.byteArray()))

fun UInt256.publishUInt256() = electionguard.protogen.UInt256(ByteArr(this.bytes))

fun ElementModP.publishElementModP() = electionguard.protogen.ElementModP(ByteArr(this.byteArray()))

fun ElGamalCiphertext.publishCiphertext() =
    electionguard.protogen.ElGamalCiphertext(
        this.pad.publishElementModP(),
        this.data.publishElementModP(),
    )

fun GenericChaumPedersenProof.publishChaumPedersenProof() =
    electionguard.protogen.GenericChaumPedersenProof(
        this.c.publishElementModQ(),
        this.r.publishElementModQ(),
    )

fun HashedElGamalCiphertext.publishHashedCiphertext() =
    electionguard.protogen.HashedElGamalCiphertext(
        this.c0.publishElementModP(),
        ByteArr(this.c1),
        this.c2.publishUInt256(),
        this.numBytes,
    )

fun SchnorrProof.publishSchnorrProof() =
    electionguard.protogen.SchnorrProof(
        this.publicKey.publishElementModP(),
        this.challenge.publishElementModQ(),
        this.response.publishElementModQ()
    )
