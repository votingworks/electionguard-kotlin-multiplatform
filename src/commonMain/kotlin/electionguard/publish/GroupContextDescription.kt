package electionguard.publish

import electionguard.core.Base64.toBase64
import io.ktor.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * A public description of the mathematical group used for the encryption and processing of ballots.
 * One of these should accompany every batch of encrypted ballots, allowing future code that might
 * process those ballots to determine what parameters were in use and possibly give a warning or
 * error if they were unexpected.
 *
 * The string values are defined to be big-endian base64-encoded versions of the primitives
 * integers.
 */
@Serializable
data class GroupContextDescription(
    val isProductionStrength: Boolean,
    val p: String,
    val q: String,
    val r: String,
    val g: String,
    val description: String
)

/** Helper function: building a [GroupContextDescription] from byte arrays. */
fun groupContextDescriptionFromBytes(
    isProductionStrength: Boolean,
    p: ByteArray,
    q: ByteArray,
    r: ByteArray,
    g: ByteArray,
    description: String
) =
    GroupContextDescription(
        isProductionStrength,
        p.toBase64(),
        q.toBase64(),
        r.toBase64(),
        g.toBase64(),
        description
    )

/**
 * Validates whether external data, possibly encrypted using obsolete group context parameters, is
 * compatible with the current group, supported by the current ElectionGuard library.
 */
fun GroupContextDescription.isCompatible(other: GroupContextDescription): Boolean =
    other.isProductionStrength == isProductionStrength && other.p == p && other.q == q &&
        other.g == g && other.r == r

/**
 * Validates whether external data, possibly encrypted using obsolete group context parameters, is
 * compatible with the current group, supported by the current ElectionGuard library. If
 * incompatible, throws a [RuntimeException].
 */
fun GroupContextDescription.requireCompatible(other: GroupContextDescription) {
    if (!isCompatible(other))
        throw RuntimeException(
            "other group (${other.description}) is incompatible with this group ($description)"
        )
}

/**
 * Given a JSON-serialized GroupContextDescription, deserializes it and validates whether it, and
 * the data processed with it, is compatible with the current group, supported by the current
 * ElectionGuard library. If incompatible, throws a [RuntimeException] or [SerializationException].
 */
fun GroupContextDescription.requireCompatible(other: JsonElement) =
    requireCompatible(Json.decodeFromJsonElement<GroupContextDescription>(other))