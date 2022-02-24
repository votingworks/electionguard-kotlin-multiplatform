package electionguard.serialize

import electionguard.core.*
import electionguard.core.Base16.fromHex
import electionguard.core.Base16.toHex
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** External representation of an ElGamal secret key */
@Serializable(with = ElGamalSecretKeyAsStringSerializer::class)
@SerialName("ElGamalSecretKey")
data class ElGamalSecretKeyPub(val key: ElementModQPub)

/** External representation of an ElGamal public key */
@Serializable(with = ElGamalPublicKeyAsStringSerializer::class)
@SerialName("ElGamalPublicKey")
data class ElGamalPublicKeyPub(val key: ElementModPPub)

/** External representation of an ElGamal keypair */
@Serializable
@SerialName("ElGamalKeypair")
data class ElGamalKeypairPub(val secretKey: ElGamalSecretKeyPub, val publicKey: ElGamalPublicKeyPub)

/** External representation of an ElGamal ciphertext */
@Serializable
@SerialName("ElGamalCiphertext")
data class ElGamalCiphertextPub(val pad: ElementModPPub, val data: ElementModPPub)

/** Publishes an [ElGamalPublicKey] to its external, serializable form. */
fun ElGamalPublicKey.publish(): ElGamalPublicKeyPub = ElGamalPublicKeyPub(this.key.publish())

/** Publishes an [ElGamalSecretKey] to its external, serializable form. */
fun ElGamalSecretKey.publish(): ElGamalSecretKeyPub = ElGamalSecretKeyPub(this.key.publish())

/** Publishes an [ElGamalKeypair] to its external, serializable form. */
fun ElGamalKeypair.publish(): ElGamalKeypairPub =
    ElGamalKeypairPub(secretKey.publish(), publicKey.publish())

/** Publishes an [ElGamalCiphertext] to its external, serializable form. */
fun ElGamalCiphertext.publish(): ElGamalCiphertextPub =
    ElGamalCiphertextPub(pad.publish(), data.publish())

/** Imports from a published [ElGamalPublicKey]. Returns `null` if it's malformed. */
fun GroupContext.import(element: ElGamalPublicKeyPub): ElGamalPublicKey? =
    this.import(element.key)?.let { ElGamalPublicKey(it) }

/** Imports from a published [ElGamalSecretKey]. Returns `null` if it's malformed. */
fun GroupContext.import(element: ElGamalSecretKeyPub): ElGamalSecretKey? =
    this.import(element.key)?.let { ElGamalSecretKey(it) }

/** Imports from a published [ElGamalKeypair]. Returns `null` if it's malformed. */
fun GroupContext.import(keypair: ElGamalKeypairPub): ElGamalKeypair? {
    val secretKey = import(keypair.secretKey)
    val publicKey = import(keypair.publicKey)
    if (secretKey == null || publicKey == null) return null
    return ElGamalKeypair(secretKey, publicKey)
}

/** Imports from a published [ElGamalCiphertext]. Returns `null` if it's malformed. */
fun GroupContext.import(ciphertext: ElGamalCiphertextPub): ElGamalCiphertext? {
    val pad = import(ciphertext.pad)
    val data = import(ciphertext.data)
    if (pad == null || data == null) return null
    return ElGamalCiphertext(pad, data)
}

/** Custom serializer for [ElGamalPublicKey] that outputs just a hex-string. */
object ElGamalPublicKeyAsStringSerializer : KSerializer<ElGamalPublicKeyPub> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ElGamalPublicKey", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ElGamalPublicKeyPub) {
        val string = value.key.value.toHex()
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): ElGamalPublicKeyPub {
        val string = decoder.decodeString()
        return ElGamalPublicKeyPub(
            ElementModPPub(
                string.fromHex() ?: throw SerializationException("invalid base16 string")
            )
        )
    }
}

/** Custom serializer for [ElGamalSecretKeyPub] that outputs just a hex-string. */
object ElGamalSecretKeyAsStringSerializer : KSerializer<ElGamalSecretKeyPub> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ElementModQ", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ElGamalSecretKeyPub) {
        val string = value.key.value.toHex()
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): ElGamalSecretKeyPub {
        val string = decoder.decodeString()
        return ElGamalSecretKeyPub(
            ElementModQPub(
                string.fromHex() ?: throw SerializationException("invalid base16 string")
            )
        )
    }
}