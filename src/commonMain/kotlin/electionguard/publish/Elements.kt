@file:OptIn(ExperimentalSerializationApi::class)

package electionguard.publish

import electionguard.core.Base16.fromHex
import electionguard.core.Base16.toHex
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
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

// We're doing custom serializers for ElementModP and ElementModP that know how to
// convert from ByteArray (our "internal" format) to a JSON object having one key
// ("value"), and the corresponding value is a big-endian base16-encoding of
// the ByteArray.

// Hopefully, we won't need to do this mess anywhere else. Instead, we'll just have
// Kotlin data classes that correspond 1:1 with the JSON structures used by
// ElectionGuard.

// Note: if we want to change from base16 to base64, just replace toHex/fromHex with
// toBase64/fromBase64 and everything else would stay the same. Also, there's a lot
// of extra complexity here to wrap the JSON object around the string. If we didn't
// care about that, and just wanted to directly have the base16 string, by itself,
// then `buildClassSerialDescriptor` could be replaced with `PrimitiveSerialDescriptor`
// and so forth.

// Associated documentation for "composite serializers":
// https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serializers.md#hand-written-composite-serializer

// Versus the simpler handling of "primitive string serializers":
// https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serializers.md#primitive-serializer

/** Custom serializer for [ElementModPPub]. */
object ElementModPAsStringSerializer : KSerializer<ElementModPPub> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("ElementModP") { element("value", serialDescriptor<String>()) }

    override fun serialize(encoder: Encoder, value: ElementModPPub) {
        val string = value.value.toHex()
        val composite = encoder.beginStructure(descriptor)
        composite.encodeStringElement(descriptor, 0, string)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): ElementModPPub {
        val composite = decoder.beginStructure(descriptor)
        val string = composite.decodeStringElement(descriptor, 0)
        composite.endStructure(descriptor)
        return ElementModPPub(
            string.fromHex() ?: throw SerializationException("invalid base16 string")
        )
    }
}

/** Custom serializer for [ElementModQPub]. */
object ElementModQAsStringSerializer : KSerializer<ElementModQPub> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("ElementModQ") { element("value", serialDescriptor<String>()) }

    override fun serialize(encoder: Encoder, value: ElementModQPub) {
        val string = value.value.toHex()
        val composite = encoder.beginStructure(descriptor)
        composite.encodeStringElement(descriptor, 0, string)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): ElementModQPub {
        val composite = decoder.beginStructure(descriptor)
        val string = composite.decodeStringElement(descriptor, 0)
        composite.endStructure(descriptor)
        return ElementModQPub(
            string.fromHex() ?: throw SerializationException("invalid base16 string")
        )
    }
}

// TODO: find a way to have these two "object" singletons written once rather than cut-and-pasted.
//   Getting instances would be straightforward (doing the Kotlin equivalent of Java's anonymous
//   classes), but we need a ::class reference for the annotation.

/** Publishes an ElementModP to its external form. */
fun ElementModP.publish(): ElementModPPub = ElementModPPub(this.byteArray())

/** Publishes an ElementModQ to its external form. */
fun ElementModQ.publish(): ElementModQPub = ElementModQPub(this.byteArray())

/** Imports from a published ElementModP. Returns `null` if it's out of bounds. */
fun GroupContext.import(element: ElementModPPub): ElementModP? = binaryToElementModP(element.value)

/** Imports from a published ElementModQ. Returns `null` if it's out of bounds. */
fun GroupContext.import(element: ElementModQPub): ElementModQ? = binaryToElementModQ(element.value)
