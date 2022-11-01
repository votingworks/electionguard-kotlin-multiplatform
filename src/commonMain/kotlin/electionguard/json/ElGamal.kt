package electionguard.json

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
data class ElGamalSecretKeyJson(val key: ElementModQJson)

/** External representation of an ElGamal public key */
@Serializable(with = ElGamalPublicKeyAsStringSerializer::class)
@SerialName("ElGamalPublicKey")
data class ElGamalPublicKeyJson(val key: ElementModPJson)

/** External representation of an ElGamal keypair */
@Serializable
@SerialName("ElGamalKeypair")
data class ElGamalKeypairJson(val secret_key: ElGamalSecretKeyJson, val public_key: ElGamalPublicKeyJson)

/** External representation of an ElGamal ciphertext */
@Serializable
@SerialName("ElGamalCiphertext")
data class ElGamalCiphertextJson(val pad: ElementModPJson, val data: ElementModPJson)

/** Custom serializer for [ElGamalPublicKey] that outputs just a hex-string. */
object ElGamalPublicKeyAsStringSerializer : KSerializer<ElGamalPublicKeyJson> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ElGamalPublicKey", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ElGamalPublicKeyJson) {
        val string = value.key.value.toHex()
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): ElGamalPublicKeyJson {
        val string = decoder.decodeString()
        return ElGamalPublicKeyJson(
            ElementModPJson(
                string.fromHex() ?: throw SerializationException("invalid base16 string")
            )
        )
    }
}

/** Custom serializer for [ElGamalSecretKeyJson] that outputs just a hex-string. */
object ElGamalSecretKeyAsStringSerializer : KSerializer<ElGamalSecretKeyJson> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ElGamalSecretKey", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ElGamalSecretKeyJson) {
        val string = value.key.value.toHex()
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): ElGamalSecretKeyJson {
        val string = decoder.decodeString()
        return ElGamalSecretKeyJson(
            ElementModQJson(
                string.fromHex() ?: throw SerializationException("invalid base16 string")
            )
        )
    }
}

/** Publishes an [ElGamalPublicKey] to its external, serializable form. */
fun ElGamalPublicKey.publish(): ElGamalPublicKeyJson = ElGamalPublicKeyJson(this.key.publishModP())

/** Publishes an [ElGamalSecretKey] to its external, serializable form. */
fun ElGamalSecretKey.publish(): ElGamalSecretKeyJson = ElGamalSecretKeyJson(this.key.publishModQ())

/** Publishes an [ElGamalKeypair] to its external, serializable form. */
fun ElGamalKeypair.publish(): ElGamalKeypairJson =
    ElGamalKeypairJson(secretKey.publish(), publicKey.publish())

/** Publishes an [ElGamalCiphertext] to its external, serializable form. */
fun ElGamalCiphertext.publish(): ElGamalCiphertextJson =
    ElGamalCiphertextJson(pad.publishModP(), data.publishModP())

/** Imports from a published [ElGamalPublicKey]. Returns `null` if it's malformed. */
fun GroupContext.importPublicKey(org: ElGamalPublicKeyJson): ElGamalPublicKey? =
    this.importModP(org.key)?.let { ElGamalPublicKey(it) }

/** Imports from a published [ElGamalSecretKey]. Returns `null` if it's malformed. */
fun GroupContext.importSecretKey(org: ElGamalSecretKeyJson): ElGamalSecretKey? =
    this.importModQ(org.key)?.let { ElGamalSecretKey(it) }

/** Imports from a published [ElGamalKeypair]. Returns `null` if it's malformed. */
fun GroupContext.importKeyPair(keypair: ElGamalKeypairJson): ElGamalKeypair? {
    val secretKey = this.importSecretKey(keypair.secret_key)
    val publicKey = this.importPublicKey(keypair.public_key)
    if (secretKey == null || publicKey == null) return null
    return ElGamalKeypair(secretKey, publicKey)
}

/** Imports from a published [ElGamalCiphertext]. Returns `null` if it's malformed. */
fun GroupContext.importElGamalCiphertext(ciphertext: ElGamalCiphertextJson): ElGamalCiphertext? {
    val pad = this.importModP(ciphertext.pad)
    val data = this.importModP(ciphertext.data)
    if (pad == null || data == null) return null
    return ElGamalCiphertext(pad, data)
}