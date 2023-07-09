package electionguard.ballot

import electionguard.core.*
import electionguard.core.Base16.toHex
import io.ktor.utils.io.core.toByteArray

const val protocolVersion = "v2.0"

/** Configuration input for KeyCeremony. */
data class ElectionConfig(
    val configVersion: String,
    val constants: ElectionConstants,

    /** The number of guardians necessary to generate the public key. */
    val numberOfGuardians: Int,
    /** The quorum of guardians necessary to decrypt an election. Must be <= numberOfGuardians. */
    val quorum: Int,
    /** date string used in hash */
    val electionDate : String,
    /** info string used in hash */
    val jurisdictionInfo : String,

    val parameterBaseHash : UInt256, // Hp
    val manifestHash : UInt256, // Hm
    val electionBaseHash : UInt256,  // Hb
    // the raw bytes of the manifest. You must regenerate the manifest from this.
    // TODO may need to specify serialization form, or detect it.
    val manifestBytes: ByteArray,

    val baux0: ByteArray, // B_aux,0 from eq 59,60
    val device: String, // the device information from eq 61, and section 3.7
    val chainConfirmationCodes: Boolean = false,

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
        if (electionDate != other.electionDate) return false
        if (jurisdictionInfo != other.jurisdictionInfo) return false
        if (parameterBaseHash != other.parameterBaseHash) return false
        if (manifestHash != other.manifestHash) return false
        if (electionBaseHash != other.electionBaseHash) return false
        if (!manifestBytes.contentEquals(other.manifestBytes)) return false
        if (!baux0.contentEquals(other.baux0)) return false
        if (device != other.device) return false
        if (chainConfirmationCodes != other.chainConfirmationCodes) return false
        return metadata == other.metadata
    }

    override fun hashCode(): Int {
        var result = configVersion.hashCode()
        result = 31 * result + constants.hashCode()
        result = 31 * result + numberOfGuardians
        result = 31 * result + quorum
        result = 31 * result + electionDate.hashCode()
        result = 31 * result + jurisdictionInfo.hashCode()
        result = 31 * result + parameterBaseHash.hashCode()
        result = 31 * result + manifestHash.hashCode()
        result = 31 * result + electionBaseHash.hashCode()
        result = 31 * result + manifestBytes.contentHashCode()
        result = 31 * result + baux0.contentHashCode()
        result = 31 * result + device.hashCode()
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
    // HP = H(HV ; 00, p, q, g)   spec 1.9, p 15, eq 4
    // The symbol HV denotes the version byte array that encodes the used version of this specification.
    // The array has length 32 and contains the UTF-8 encoding of the string "v2.0" followed by 00-
    // bytes, i.e. HV = 76322E30 ∥ b(0, 28).
    val version = protocolVersion.toByteArray()
    val HV = ByteArray(32) { if (it < 4) version[it] else 0 }

    return hashFunction(
        HV,
        0x00.toByte(),
        primes.largePrime,
        primes.smallPrime,
        primes.generator,
    )
}

fun manifestHash(Hp: UInt256, manifestBytes : ByteArray) : UInt256 {
    // HM = H(HP ; 01, manifest).   spec 1.9, p 16, eq 5
    return hashFunction(
        Hp.bytes,
        0x01.toByte(),
        manifestBytes,
    )
}

fun electionBaseHash(Hp: UInt256, n : Int, k : Int, date : String, info : String, HM: UInt256) : UInt256 {
    // HB = H(HP ; 02, n, k, date, info, HM ).   spec 1.9, p 17, eq 6
    return hashFunction(
        Hp.bytes,
        0x02.toByte(),
        n.toUShort(),
        k.toUShort(),
        date,
        info,
        HM.bytes,
    )
}

/** Make ElectionConfig, calculating Hp, Hm and Hb. */
fun makeElectionConfig(
    configVersion: String,
    constants: ElectionConstants,
    numberOfGuardians: Int,
    quorum: Int,
    electionDate: String,
    jurisdictionInfo: String,
    manifestBytes: ByteArray,
    baux0: ByteArray, // B_aux,0 from eq 59,60
    device: String, // the device information from eq 61, and section 3.7
    chainConfirmationCodes: Boolean = false,
    metadata: Map<String, String> = emptyMap(),
): ElectionConfig {

    val parameterBaseHash = parameterBaseHash(constants)
    val manifestHash = manifestHash(parameterBaseHash, manifestBytes)
    val electionBaseHash = electionBaseHash(parameterBaseHash, numberOfGuardians, quorum, electionDate, jurisdictionInfo, manifestHash)

    return ElectionConfig(
        configVersion,
        constants,
        numberOfGuardians,
        quorum,
        electionDate,
        jurisdictionInfo,
        parameterBaseHash,
        manifestHash,
        electionBaseHash,
        manifestBytes,
        baux0,
        device,
        chainConfirmationCodes,
        metadata,
    )
}