package electionguard.publish

import electionguard.core.Base16.toHex
import io.ktor.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import mu.KotlinLogging

private val logger = KotlinLogging.logger("Constants")

/**
 * A public description of the mathematical group used for the encryption and processing of ballots.
 * One of these should accompany every batch of encrypted ballots, allowing future code that might
 * process those ballots to determine what parameters were in use and possibly give a warning or
 * error if they were unexpected.
 *
 * The string values are defined to be big-endian base16-encoded versions of the primitives
 * integers.
 */
@Serializable
data class Constants(
    val large_prime: String,
    val small_prime: String,
    val cofactor: String,
    val generator: String,
)

/** Helper function: building a [Constants] from byte arrays. */
fun constantsFromBytes(p: ByteArray, q: ByteArray, r: ByteArray, g: ByteArray) =
    Constants(p.toHex(), q.toHex(), r.toHex(), g.toHex())

/**
 * Validates whether external data, possibly encrypted using obsolete group context parameters, is
 * compatible with the current group, supported by the current ElectionGuard library.
 */
fun Constants.isCompatible(other: Constants): Boolean =
    other.large_prime == large_prime && other.small_prime == small_prime &&
        other.generator == generator && other.cofactor == cofactor

/**
 * Validates whether external data, possibly encrypted using obsolete group context parameters, is
 * compatible with the current group, supported by the current ElectionGuard library. If
 * incompatible, throws a [RuntimeException].
 */
fun Constants.requireCompatible(other: Constants) {
    if (!isCompatible(other)) {
        val errStr =
            "other group is incompatible with this group: " +
                Json.encodeToString(mapOf("other" to other, "this" to this))
        logger.warn { errStr }
        throw RuntimeException(errStr)
    }
}

/**
 * Given a JSON-serialized [Constants], deserializes it and validates whether it, and the data
 * processed with it, is compatible with the current group, supported by the current ElectionGuard
 * library. If incompatible, throws a [RuntimeException] or [SerializationException].
 */
fun Constants.requireCompatible(other: JsonElement) =
    requireCompatible(Json.decodeFromJsonElement<Constants>(other))