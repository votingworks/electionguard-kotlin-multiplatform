package electionguard.ballot

/** Configuration for KeyCeremony. */
data class ElectionConfig(
    val protoVersion: String,
    val constants: Constants,
    val manifest: Manifest,
    /** The number of guardians necessary to generate the public key. */
    val numberOfGuardians: Int,
    /** The quorum of guardians necessary to decrypt an election. Must be <= number_of_guardians. */
    val quorum: Int,
    /** arbitrary key/value metadata. */
    val metadata: Map<String, String>,
)

/**
 * A public description of the mathematical group used for the encryption and processing of ballots.
 * The byte arrays are defined to be big-endian.
 */
data class Constants(
    val name: String,
    /** large prime or P. */
    val largePrime: ByteArray,
    /** small prime or Q. */
    val smallPrime: ByteArray,
    /** cofactor or R. */
    val cofactor: ByteArray,
    /** generator or G. */
    val generator: ByteArray,
)