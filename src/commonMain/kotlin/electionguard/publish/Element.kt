package electionguard.publish

import electionguard.core.Base16.fromHex
import electionguard.core.Base16.toHex
import electionguard.core.GroupContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** External representation of an ElementModP. */
@Serializable(with = ElementModPAsStringSerializer::class)
@SerialName("ElementModP")
data class ElementModPPub(val value: ByteArray)

/** External representation of an ElementModQ. */
@Serializable(with = ElementModQAsStringSerializer::class)
@SerialName("ElementModQ")
data class ElementModQPub(val value: ByteArray)

// TODO: do we need to serialize to a string, as we're doing now, or to a JSON Object with some key
//  and value?

object ElementModPAsStringSerializer : KSerializer<ElementModPPub> {
    // if we want to change from base16 to base64, just replace toHex/fromHex with
    // toBase64/fromBase64

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ElementModP", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ElementModPPub) {
        val string = value.value.toHex()
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): ElementModPPub {
        val string = decoder.decodeString()
        return ElementModPPub(
            string.fromHex() ?: throw SerializationException("invalid base16 string")
        )
    }
}

object ElementModQAsStringSerializer : KSerializer<ElementModQPub> {
    // if we want to change from base16 to base64, just replace toHex/fromHex with
    // toBase64/fromBase64

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ElementModQ", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ElementModQPub) {
        val string = value.value.toHex()
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): ElementModQPub {
        val string = decoder.decodeString()
        return ElementModQPub(
            string.fromHex() ?: throw SerializationException("invalid base16 string")
        )
    }
}

/** Publishes an ElementModP to its external form. */
fun electionguard.core.ElementModP.publish(): ElementModPPub = ElementModPPub(this.byteArray())

/** Publishes an ElementModQ to its external form. */
fun electionguard.core.ElementModQ.publish(): ElementModQPub = ElementModQPub(this.byteArray())

/** Imports from a published ElementModP. Returns `null` if it's out of bounds. */
fun GroupContext.import(element: ElementModPPub): electionguard.core.ElementModP? =
    binaryToElementModP(element.value)

/** Imports from a published ElementModQ. Returns `null` if it's out of bounds. */
fun GroupContext.import(element: ElementModQPub): electionguard.core.ElementModQ? =
    binaryToElementModQ(element.value)
