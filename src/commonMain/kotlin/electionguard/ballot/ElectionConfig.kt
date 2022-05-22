package electionguard.ballot

/** Configuration for KeyCeremony. */
data class ElectionConfig(
    val protoVersion: String,
    val constants: ElectionConstants,
    val manifest: Manifest,
    /** The number of guardians necessary to generate the public key. */
    val numberOfGuardians: Int,
    /** The quorum of guardians necessary to decrypt an election. Must be <= number_of_guardians. */
    val quorum: Int,
    /** arbitrary key/value metadata. */
    val metadata: Map<String, String> = emptyMap(),
)

/**
 * A public description of the mathematical group used for the encryption and processing of ballots.
 * The byte arrays are defined to be big-endian.
 */
data class ElectionConstants(
    val name: String,
    /** large prime or P. */
    val largePrime: ByteArray,
    /** small prime or Q. */
    val smallPrime: ByteArray,
    /** cofactor or R. */
    val cofactor: ByteArray,
    /** generator or G. */
    val generator: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ElectionConstants

        if (name != other.name) return false
        if (!largePrime.contentEquals(other.largePrime)) return false
        if (!smallPrime.contentEquals(other.smallPrime)) return false
        if (!cofactor.contentEquals(other.cofactor)) return false
        if (!generator.contentEquals(other.generator)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + largePrime.contentHashCode()
        result = 31 * result + smallPrime.contentHashCode()
        result = 31 * result + cofactor.contentHashCode()
        result = 31 * result + generator.contentHashCode()
        return result
    }
}