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
fun GroupContext.importElementModQ(modQ: electionguard.protogen.ElementModQ?): ElementModQ? =
    if (modQ == null) null else this.binaryToElementModQ(modQ.value.array)

fun importUInt256(modQ: electionguard.protogen.UInt256?): UInt256? =
    if (modQ == null) null else modQ.value.array.toUInt256()

/**
 * Converts a deserialized ElementModP to the internal representation, returning `null` if the input
 * was missing, out of bounds, or otherwise malformed. `null` is accepted as an input, for
 * convenience. If `null` is passed as input, `null` is returned.
 */
fun GroupContext.importElementModP(modP: electionguard.protogen.ElementModP?,): ElementModP? =
    if (modP == null) null else this.binaryToElementModP(modP.value.array)

fun GroupContext.importCiphertext(
    ciphertext: electionguard.protogen.ElGamalCiphertext?,
): ElGamalCiphertext? {
    if (ciphertext == null) return null

    val pad = this.importElementModP(ciphertext.pad)
    val data = this.importElementModP(ciphertext.data)

    if (pad == null || data == null) {
        logger.error { "ElGamalCiphertext pad or value was malformed or out of bounds" }
        return null
    }

    return ElGamalCiphertext(pad, data)
}

fun GroupContext.importChaumPedersenProof(
    proof: electionguard.protogen.GenericChaumPedersenProof?,
): GenericChaumPedersenProof? {
    // TODO: this should probably be a ConstantChaumPedersenProofKnownSecretKey, which needs to know
    //   what the constant actually is. That should be nearby in the serialized data.

    if (proof == null) return null

    val challenge = this.importElementModQ(proof.challenge)
    val response = this.importElementModQ(proof.response)

    if (challenge == null || response == null) {
        logger.error {
            "GenericChaumPedersenProof fields are missing or malformed or out of bounds"
        }
        return null
    }

    return GenericChaumPedersenProof(challenge, response)
}

fun GroupContext.importHashedCiphertext(
    ciphertext: electionguard.protogen.HashedElGamalCiphertext?,
): HashedElGamalCiphertext? {
    if (ciphertext == null) return null

    val c0 = this.importElementModP(ciphertext.c0)
    val c2 = importUInt256(ciphertext.c2)

    if (c0 == null || c2 == null) {
        logger.error { "HashedElGamalCiphertext fields missing" }
        return null
    }

    return HashedElGamalCiphertext(c0, ciphertext.c1.array, c2, ciphertext.numBytes)
}

fun GroupContext.importSchnorrProof(proof: electionguard.protogen.SchnorrProof?): SchnorrProof? {

    if (proof == null) return null

    val challenge = this.importElementModQ(proof.challenge)
    val response = this.importElementModQ(proof.response)

    if (challenge == null || response == null) {
        logger.error { "one or more SchnorrProof inputs was malformed or out of bounds" }
        return null
    }

    return SchnorrProof(challenge, response)
}

fun GroupContext.importElGamalPublicKey(
    publicKey: electionguard.protogen.ElementModP?,
) : ElGamalPublicKey? {
    val key = this.importElementModP(publicKey)
    if (key == null) {
        logger.error { "ElGamalPublicKey was malformed or out of bounds" }
        return null
    }
    return ElGamalPublicKey(key)
}

///////////////////////////////////////////////////////////////////////////////////////////////////////

fun ElementModQ.publishElementModQ(): electionguard.protogen.ElementModQ {
    return electionguard.protogen.ElementModQ(ByteArr(this.byteArray()))
}

fun UInt256.publishUInt256(): electionguard.protogen.UInt256 {
    val ba = this.bytes
    val bba = ByteArr(ba)
    val uq = electionguard.protogen.UInt256(bba)
    return uq
    // return electionguard.protogen.UInt256(ByteArr(this.bytes))
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
                null,
                null, // 1.0 0nly
                this.c.publishElementModQ(),
                this.r.publishElementModQ(),
            )
    }

fun HashedElGamalCiphertext.publishHashedCiphertext() :
    electionguard.protogen.HashedElGamalCiphertext {
        return electionguard.protogen
            .HashedElGamalCiphertext(
                this.c0.publishElementModP(),
                ByteArr(this.c1),
                this.c2.publishUInt256(),
                this.numBytes,
            )
    }

fun SchnorrProof.publishSchnorrProof(): electionguard.protogen.SchnorrProof {
    return electionguard.protogen
        .SchnorrProof(
            null,
            null, // 1.0 0nly
            this.challenge.publishElementModQ(),
            this.response.publishElementModQ()
        )
}

fun ElGamalPublicKey.publishElGamalPublicKey() : electionguard.protogen.ElementModP {
    return this.key.publishElementModP()
}