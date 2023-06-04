package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.core.*
import pbandk.ByteArr

// Note that importXXX(protogen.T?) return T?, while T.publishProto() return protogen.T
// Its up to the calling routines to turn that into Result<Boolean, String>

fun GroupContext.importElementModQ(modQ: electionguard.protogen.ElementModQ?): ElementModQ? =
    modQ?.let { this.binaryToElementModQ(modQ.value.array) }

fun importUInt256(modQ: electionguard.protogen.UInt256?): UInt256? =
    modQ?.value?.array?.toUInt256()

fun electionguard.protogen.UInt256.import(): Result<UInt256, String> {
    val result = this.value?.array?.toUInt256()
    return if (result != null) Ok(result) else Err("malformed UInt256") // TODO
}

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

fun ElementModQ.publishProto() = electionguard.protogen.ElementModQ(ByteArr(this.byteArray()))

fun UInt256.publishProto() = electionguard.protogen.UInt256(ByteArr(this.bytes))

fun ElementModP.publishProto() = electionguard.protogen.ElementModP(ByteArr(this.byteArray()))

fun ElGamalCiphertext.publishProto() =
    electionguard.protogen.ElGamalCiphertext(
        this.pad.publishProto(),
        this.data.publishProto(),
    )

fun GenericChaumPedersenProof.publishProto() =
    electionguard.protogen.GenericChaumPedersenProof(
        this.c.publishProto(),
        this.r.publishProto(),
    )

fun HashedElGamalCiphertext.publishProto() =
    electionguard.protogen.HashedElGamalCiphertext(
        this.c0.publishProto(),
        ByteArr(this.c1),
        this.c2.publishProto(),
        this.numBytes,
    )

fun SchnorrProof.publishProto() =
    electionguard.protogen.SchnorrProof(
        this.publicKey.publishProto(),
        this.challenge.publishProto(),
        this.response.publishProto()
    )
