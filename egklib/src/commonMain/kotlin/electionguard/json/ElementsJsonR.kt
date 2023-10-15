package electionguard.json

import electionguard.core.Base16.fromHex
import electionguard.core.Base16.toHex
import electionguard.core.Base64.fromBase64
import electionguard.core.Base64.toBase64
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.UInt256
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** External representation of an ElementModP. */
@Serializable(with = ElementModPR::class)
@SerialName("ElementModPR")
data class ElementModPJsonR(val bytes: ByteArray) {
    override fun toString() = bytes.toHex()
}

/** Custom serializer for [ElementModPR]. */
object ElementModPR : KSerializer<ElementModPJsonR> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ElementModPR", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ElementModPJsonR) {
        val base64 = value.bytes.toBase64()
        encoder.encodeString("base64:$base64")
    }

    override fun deserialize(decoder: Decoder): ElementModPJsonR {
        val base64withHeader = decoder.decodeString()
        require(base64withHeader.startsWith("base64:"))
        val base64 = base64withHeader.substring(7)
        return ElementModPJsonR(
            base64.fromBase64() ?: throw Throwable("invalid base64 string")
        )
    }
}

/** Publishes an ElementModP to its external, serializable form. */
fun ElementModP.publishJsonR(): ElementModPJsonR = ElementModPJsonR(this.byteArray())

// TODO these throw RuntimeException instead of T?
fun ElementModPJsonR.import(group: GroupContext): ElementModP {
    val wtf = group.binaryToElementModP(this.bytes)
    return wtf!! // TODO
}

///////////////////////////////////////////////////////////////////////////////////////////////

/** External representation of an ElementModQ. */
@Serializable(with = ElementModQR::class)
@SerialName("ElementModQR")
data class ElementModQJsonR(val bytes: ByteArray) {
    override fun toString() = bytes.toHex()
}

/** Custom serializer for [ElementModQR]. */
object ElementModQR : KSerializer<ElementModQJsonR> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ElementModQ", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ElementModQJsonR) {
        val base64 = value.bytes.toBase64()
        encoder.encodeString("base64:$base64")
    }

    override fun deserialize(decoder: Decoder): ElementModQJsonR {
        val base64withHeader = decoder.decodeString()
        require(base64withHeader.startsWith("base64:"))
        val base64 = base64withHeader.substring(7)
        return ElementModQJsonR(
            base64.fromBase64() ?: throw Throwable("invalid base64 string")
        )
    }
}


/** Publishes an ElementModQ to its external, serializable form. */
fun ElementModQ.publishJsonR(): ElementModQJsonR = ElementModQJsonR(this.byteArray())

fun ElementModQJsonR.import(group: GroupContext): ElementModQ = group.binaryToElementModQ(this.bytes)?: throw RuntimeException()

///////////////////////////////////////////////////////////////////////////////////////////////////////

/** External representation of a UInt256. */
@Serializable(with = UInt256R::class)
@SerialName("UInt256R")
data class UInt256JsonR(val bytes: ByteArray) {
    override fun toString() = bytes.toHex()
}

/** Custom serializer for [UInt256JsonR]. */
object UInt256R : KSerializer<UInt256JsonR> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UInt256", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UInt256JsonR) {
        val base16 = value.bytes.toHex()
        encoder.encodeString("H($base16)")
    }

    override fun deserialize(decoder: Decoder): UInt256JsonR {
        val base16withWrapper = decoder.decodeString()
        require(base16withWrapper.startsWith("H("))
        val len = base16withWrapper.length
        val base16 = base16withWrapper.substring(2, len-1)
        return UInt256JsonR(
            base16.fromHex() ?: throw Throwable("invalid base16 string")
        )
    }
}

/** Publishes an UInt256 to its external, serializable form. */
fun UInt256.publishJsonR(): UInt256JsonR = UInt256JsonR(this.bytes)

fun UInt256JsonR.import(): UInt256 = if (this.bytes.size == 32) UInt256(this.bytes) else throw RuntimeException()

