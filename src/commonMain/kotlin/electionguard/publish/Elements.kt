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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

/** External representation of an ElementModP. */
@Serializable(with = ElementModPAsStringSerializer::class)
@SerialName("ElementModP")
data class ElementModPPub(val value: ByteArray)

/** External representation of an ElementModQ. */
@Serializable(with = ElementModQAsStringSerializer::class)
@SerialName("ElementModQ")
data class ElementModQPub(val value: ByteArray)

// We're doing custom serializers for ElementModP and ElementModP that know how to
// convert from ByteArray (our "internal" format) to a JSON object which is just
// a base16 string representation.

// Note: if we want to change from base16 to base64, just replace toHex/fromHex with
// toBase64/fromBase64 and everything else would stay the same.

// Associated documentation for "composite serializers":
// https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serializers.md#hand-written-composite-serializer

// Versus the simpler handling of "primitive string serializers":
// https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serializers.md#primitive-serializer

/** Custom serializer for [ElementModPPub]. */
object ElementModPAsStringSerializer : KSerializer<ElementModPPub> {
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

/** Custom serializer for [ElementModQPub]. */
object ElementModQAsStringSerializer : KSerializer<ElementModQPub> {
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

/** Publishes an ElementModP to its external, serializable form. */
fun ElementModP.publish(): ElementModPPub = ElementModPPub(this.byteArray())

/** Publishes an ElementModQ to its external, serializable form. */
fun ElementModQ.publish(): ElementModQPub = ElementModQPub(this.byteArray())

/** Publishes an ElementModP to a JSON AST representation. */
fun ElementModP.publishJson(): JsonElement = Json.encodeToJsonElement(publish())

/** Publishes an ElementModQ to a JSON AST representation. */
fun ElementModQ.publishJson(): JsonElement = Json.encodeToJsonElement(publish())

/** Imports from a published ElementModP. Returns `null` if it's out of bounds. */
fun GroupContext.import(element: ElementModPPub): ElementModP? = binaryToElementModP(element.value)

/** Imports from a published ElementModQ. Returns `null` if it's out of bounds. */
fun GroupContext.import(element: ElementModQPub): ElementModQ? = binaryToElementModQ(element.value)

/** Imports from a published ElementModP. Returns `null` if it's out of bounds or malformed. */
fun GroupContext.importElementModP(element: JsonElement): ElementModP? =
    try {
        this.import(Json.decodeFromJsonElement<ElementModPPub>(element))
    } catch (ex: SerializationException) {
        // should we log this failure somewhere?
        null
    }

/** Imports from a published ElementModQ. Returns `null` if it's out of bounds or malformed.. */
fun GroupContext.importElementModQ(element: JsonElement): ElementModQ? =
    try {
        this.import(Json.decodeFromJsonElement<ElementModQPub>(element))
    } catch (ex: SerializationException) {
        // should we log this failure somewhere?
        null
    }
