package electionguard.ballot

import electionguard.core.*
import electionguard.core.Base16.toHex
import io.ktor.utils.io.core.*

const val protocolVersion = "v2.0"

/** Configuration input for KeyCeremony. */
data class ElectionConfig(
    val configVersion: String,
    val constants: ElectionConstants,
    val manifestFile: ByteArray, // the exact bytes of the original manifest File
    val manifest: Manifest, // the parsed objects

    /** The number of guardians necessary to generate the public key. */
    val numberOfGuardians: Int,
    /** The quorum of guardians necessary to decrypt an election. Must be <= numberOfGuardians. */
    val quorum: Int,
    /** date string used in hash */
    val electionDate : String,
    /** info string used in hash */
    val jurisdictionInfo : String,

    /** arbitrary key/value metadata. */
    val metadata: Map<String, String> = emptyMap(),

    /** may be calculated or passed in */
    val parameterBaseHash : UInt256 = parameterBaseHash(constants), // Hp
    val manifestHash : UInt256 = manifestHash(parameterBaseHash, manifestFile), // Hm
    val electionBaseHash : UInt256 =  // Hb
        electionBaseHash(parameterBaseHash, numberOfGuardians, quorum, electionDate, jurisdictionInfo, manifestHash),
) {
    init {
        require(numberOfGuardians > 0)  { "numberOfGuardians ${numberOfGuardians} <= 0" }
        require(numberOfGuardians >= quorum) { "numberOfGuardians ${numberOfGuardians} != $quorum" }
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
        0.toByte(),
        primes.largePrime,
        primes.smallPrime,
        primes.generator,
    )
}

fun manifestHash(Hp: UInt256, manifestFile : ByteArray) : UInt256 {
    // HM = H(HP ; 01, manifest).   spec 1.9, p 16, eq 5
    return hashFunction(
        Hp.bytes,
        1.toByte(),
        manifestFile,
    )
}

fun electionBaseHash(Hp: UInt256, n : Int, k : Int, date : String, info : String, HM: UInt256) : UInt256 {
    // HB = H(HP ; 02, n, k, date, info, HM ).   spec 1.9, p 17, eq 6
    return hashFunction(
        Hp.bytes,
        2.toByte(),
        n.toUShort(),
        k.toUShort(),
        date,
        info,
        HM.bytes,
    )
}