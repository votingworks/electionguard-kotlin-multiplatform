package electionguard.json2

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
data class ElementModPJson(val bytes: ByteArray) {
    override fun toString() = bytes.toHex()
}

/** External representation of an ElementModQ. */
@Serializable(with = ElementModQAsStringSerializer::class)
@SerialName("ElementModQ")
data class ElementModQJson(val bytes: ByteArray) {
    override fun toString() = bytes.toHex()
}

/** External representation of a UInt256. */
@Serializable(with = UInt256AsStringSerializer::class)
@SerialName("UInt256")
data class UInt256Json(val bytes: ByteArray) {
    override fun toString() = bytes.toHex()
}

/** Custom serializer for [ElementModP]. */
object ElementModPAsStringSerializer : KSerializer<ElementModPJson> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ElementModP", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ElementModPJson) {
        val string = value.bytes.toHex()
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): ElementModPJson {
        val string = decoder.decodeString()
        return ElementModPJson(
            string.fromHex() ?: throw Throwable("invalid base16 string")
        )
    }
}

/** Custom serializer for [ElementModQ]. */
object ElementModQAsStringSerializer : KSerializer<ElementModQJson> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ElementModQ", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ElementModQJson) {
        val string = value.bytes.toHex()
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): ElementModQJson {
        val string = decoder.decodeString()
        return ElementModQJson(
            string.fromHex() ?: throw Throwable("invalid base16 string")
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
            string.fromHex() ?: throw Throwable("invalid base16 string")
        )
    }
}

// Note that Tjson.import([group]) return T, while T.publishJson() returns Tjson
// TODO use Result?

/** Publishes an ElementModP to its external, serializable form. */
fun ElementModP.publishJson(): ElementModPJson = ElementModPJson(this.byteArray())

/** Publishes an ElementModQ to its external, serializable form. */
fun ElementModQ.publishJson(): ElementModQJson = ElementModQJson(this.byteArray())

/** Publishes an UInt256 to its external, serializable form. */
fun UInt256.publishJson(): UInt256Json = UInt256Json(this.bytes)

// TODO these throw RuntimeException instead of T?
fun ElementModPJson.import(group: GroupContext): ElementModP = group.binaryToElementModP(this.bytes)?: throw RuntimeException()

fun ElementModQJson.import(group: GroupContext): ElementModQ = group.binaryToElementModQ(this.bytes)?: throw RuntimeException()

fun UInt256Json.import(): UInt256 = if (this.bytes.size == 32) UInt256(this.bytes) else throw RuntimeException()

