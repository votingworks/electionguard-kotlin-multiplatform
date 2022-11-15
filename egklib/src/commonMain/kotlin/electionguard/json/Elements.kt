package electionguard.json

import electionguard.core.Base16.fromHex
import electionguard.core.Base16.toHex
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.UInt256
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** External representation of an ElementModP. */
@Serializable(with = ElementModPAsStringSerializer::class)
@SerialName("ElementModP")
data class ElementModPJson(val value: ByteArray)

/** External representation of an ElementModQ. */
@Serializable(with = ElementModQAsStringSerializer::class)
@SerialName("ElementModQ")
data class ElementModQJson(val value: ByteArray)

/** External representation of an UInt256. */
@Serializable(with = UInt256AsStringSerializer::class)
@SerialName("UInt256")
data class UInt256Json(val bytes: ByteArray)

/** Custom serializer for [ElementModP]. */
object ElementModPAsStringSerializer : KSerializer<ElementModPJson> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ElementModP", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ElementModPJson) {
        val string = value.value.toHex()
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): ElementModPJson {
        val string = decoder.decodeString()
        return ElementModPJson(
            string.fromHex() ?: throw SerializationException("invalid base16 string")
        )
    }
}

/** Custom serializer for [ElementModQ]. */
object ElementModQAsStringSerializer : KSerializer<ElementModQJson> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ElementModQ", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ElementModQJson) {
        val string = value.value.toHex()
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): ElementModQJson {
        val string = decoder.decodeString()
        return ElementModQJson(
            string.fromHex() ?: throw SerializationException("invalid base16 string")
        )
    }
}

/** Custom serializer for [UInt256Json]. */
object UInt256AsStringSerializer : KSerializer<UInt256Json> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UInt256", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UInt256Json) {
        val string = value.bytes.toHex()
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): UInt256Json {
        val string = decoder.decodeString()
        return UInt256Json(
            string.fromHex() ?: throw SerializationException("invalid base16 string")
        )
    }
}

/** Publishes an ElementModP to its external, serializable form. */
fun ElementModP.publishModP(): ElementModPJson = ElementModPJson(this.byteArray())

/** Publishes an ElementModQ to its external, serializable form. */
fun ElementModQ.publishModQ(): ElementModQJson = ElementModQJson(this.byteArray())

/** Publishes an UInt256 to its external, serializable form. */
fun UInt256.publish(): UInt256Json = UInt256Json(this.bytes)

/** Imports from a published ElementModP. Returns `null` if it's out of bounds. */
fun GroupContext.importModP(element: ElementModPJson): ElementModP? = binaryToElementModP(element.value)

/** Imports from a published ElementModQ. Returns `null` if it's out of bounds. */
fun GroupContext.importModQ(element: ElementModQJson): ElementModQ? = binaryToElementModQ(element.value)

/** Imports from a published UInt256. Returns `null` if it's out of bounds. */
fun UInt256Json.import(): UInt256? = UInt256(this.bytes)

