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

// Note that importXXX() return T?, while publishXXX() return T(Json)
// Its up to the calling routines to turn that into Result<Boolean, String>

fun ElGamalPublicKey.publish(): ElGamalPublicKeyJson = ElGamalPublicKeyJson(this.key.publish())

fun ElGamalSecretKey.publish(): ElGamalSecretKeyJson = ElGamalSecretKeyJson(this.key.publish())

fun ElGamalKeypair.publish(): ElGamalKeypairJson =
    ElGamalKeypairJson(secretKey.publish(), publicKey.publish())

fun ElGamalCiphertext.publish(): ElGamalCiphertextJson =
    ElGamalCiphertextJson(pad.publish(), data.publish())

fun ElGamalPublicKeyJson.import(group: GroupContext): ElGamalPublicKey? =
    this.key.import(group)?.let { ElGamalPublicKey(it) }

fun ElGamalSecretKeyJson.import(group: GroupContext): ElGamalSecretKey? =
    this.key.import(group)?.let { ElGamalSecretKey(it) }

fun ElGamalKeypairJson.import(group: GroupContext): ElGamalKeypair? {
    val secretKey = this.secret_key.import(group)
    val publicKey = this.public_key.import(group)
    return if (secretKey == null || publicKey == null) null else  ElGamalKeypair(secretKey, publicKey)
}

fun ElGamalCiphertextJson.import(group: GroupContext): ElGamalCiphertext? {
    val pad = this.pad.import(group)
    val data = this.data.import(group)
    return if (pad == null || data == null) null else ElGamalCiphertext(pad, data)
}