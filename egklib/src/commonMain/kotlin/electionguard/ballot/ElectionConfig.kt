package electionguard.ballot

import electionguard.core.*
import electionguard.core.Base16.toHex

const val protocolVersion = "v2.0.0"

/** Configuration input for KeyCeremony. */
data class ElectionConfig(
    val configVersion: String,
    val constants: ElectionConstants,

    /** The number of guardians necessary to generate the public key. */
    val numberOfGuardians: Int,
    /** The quorum of guardians necessary to decrypt an election. Must be <= numberOfGuardians. */
    val quorum: Int,

    val parameterBaseHash : UInt256, // Hp
    val manifestHash : UInt256, // Hm
    val electionBaseHash : UInt256,  // Hb
    // the raw bytes of the manifest. You must regenerate the manifest from this.
    // TODO may need to specify serialization form, or detect it.
    val manifestBytes: ByteArray,

    val chainConfirmationCodes: Boolean = false,
    val configBaux0: ByteArray, // B_aux,0 from eq 59,60 if chain_confirmation_codes = false

    /** arbitrary key/value metadata. */
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(numberOfGuardians > 0)  { "numberOfGuardians ${numberOfGuardians} <= 0" }
        require(numberOfGuardians >= quorum) { "numberOfGuardians ${numberOfGuardians} != $quorum" }
    }

    // override because of the byte arrays
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ElectionConfig) return false

        if (configVersion != other.configVersion) return false
        if (constants != other.constants) return false
        if (numberOfGuardians != other.numberOfGuardians) return false
        if (quorum != other.quorum) return false
        if (parameterBaseHash != other.parameterBaseHash) return false
        if (manifestHash != other.manifestHash) return false
        if (electionBaseHash != other.electionBaseHash) return false
        if (!manifestBytes.contentEquals(other.manifestBytes)) return false
        if (!configBaux0.contentEquals(other.configBaux0)) return false
        if (chainConfirmationCodes != other.chainConfirmationCodes) return false
        return metadata == other.metadata
    }

    override fun hashCode(): Int {
        var result = configVersion.hashCode()
        result = 31 * result + constants.hashCode()
        result = 31 * result + numberOfGuardians
        result = 31 * result + quorum
        result = 31 * result + parameterBaseHash.hashCode()
        result = 31 * result + manifestHash.hashCode()
        result = 31 * result + electionBaseHash.hashCode()
        result = 31 * result + manifestBytes.contentHashCode()
        result = 31 * result + configBaux0.contentHashCode()
        result = 31 * result + chainConfirmationCodes.hashCode()
        result = 31 * result + metadata.hashCode()
        return result
    }

}

/**
 * A public description of the mathematical group used for the encryption and processing of ballots.
 * The byte arrays are defined to be big-endian.
 */
data class ElectionConstants(
    /** name of the constants defining the Group*/
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
    val hp = parameterBaseHash(this)

    // override because of the byte arrays
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

    override fun toString(): String {
        return "name = ${this.name}\n" +
                "largePrime = ${this.largePrime.toHex()}\n" +
                "smallPrime = ${this.smallPrime.toHex()}\n" +
                "  cofactor = ${this.cofactor.toHex()}\n" +
                " generator = ${this.generator.toHex()}\n"
    }
}

fun parameterBaseHash(primes : ElectionConstants) : UInt256 {
    // HP = H(ver; 0x00, p, q, g) ; spec 2.0.0 p 16, eq 4
    // The symbol ver denotes the version byte array that encodes the used version of this specification.
    // The array has length 32 and contains the UTF-8 encoding of the string “v2.0.0” followed by 0x00-
    // bytes, i.e. ver = 0x76322E302E30 ∥ b(0, 27). FIX should be b(0, 26)
    val version = protocolVersion.encodeToByteArray()
    val HV = ByteArray(32) { if (it < version.size) version[it] else 0 }

    return hashFunction(
        HV,
        0x00.toByte(),
        primes.largePrime,
        primes.smallPrime,
        primes.generator,
    )
}

fun manifestHash(Hp: UInt256, manifestBytes : ByteArray) : UInt256 {
    // HM = H(HP ; 0x01, manifest). spec 2.0.0 p 19, eq 6
    return hashFunction(
        Hp.bytes,
        0x01.toByte(),
        manifestBytes.size, // b(len(file), 4) ∥ b(file, len(file)) , section 5.1.5
        manifestBytes,
    )
}

fun electionBaseHash(Hp: UInt256, HM: UInt256, n : Int, k : Int) : UInt256 {
    // HB = H(HP ; 0x02, HM , n, k). spec 2.0.0 p 19, eq 7
    return hashFunction(
        Hp.bytes,
        0x02.toByte(),
        HM.bytes,
        n,
        k,
    )
}

/** Make ElectionConfig, calculating Hp, Hm and Hb. */
fun makeElectionConfig(
    configVersion: String,
    constants: ElectionConstants,
    numberOfGuardians: Int,
    quorum: Int,
    manifestBytes: ByteArray,
    chainConfirmationCodes: Boolean,
    baux0: ByteArray, // B_aux,0 from eq 58-60
    metadata: Map<String, String> = emptyMap(),
): ElectionConfig {

    val parameterBaseHash = parameterBaseHash(constants)
    val manifestHash = manifestHash(parameterBaseHash, manifestBytes)
    val electionBaseHash = electionBaseHash(parameterBaseHash, manifestHash, numberOfGuardians, quorum)

    return ElectionConfig(
        configVersion,
        constants,
        numberOfGuardians,
        quorum,
        parameterBaseHash,
        manifestHash,
        electionBaseHash,
        manifestBytes,
        chainConfirmationCodes,
        baux0,
        metadata,
    )
}