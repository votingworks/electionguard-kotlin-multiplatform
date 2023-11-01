package electionguard.publish

import com.github.michaelbull.result.Result
import electionguard.ballot.*
import electionguard.core.ElementModP
import electionguard.core.UInt256
import electionguard.util.ErrorMessages

/** Interface to the published election record.  */
interface ElectionRecord {
    enum class Stage { CONFIG, INIT, ENCRYPTED, TALLIED, DECRYPTED, }

    fun stage() : Stage
    fun topdir() : String
    fun isJson(): Boolean

    fun constants(): ElectionConstants
    fun manifest(): Manifest
    fun manifestBytes(): ByteArray

    /** The number of guardians necessary to generate the public key. */
    fun numberOfGuardians(): Int
    /** The quorum of guardians necessary to decrypt an election. Must be <= number_of_guardians. */
    fun quorum(): Int
    fun config(): ElectionConfig

    /** The parameter base hash, Hp. */
    fun parameterBaseHash(): UInt256
    /** The base hash, Hb. */
    fun electionBaseHash(): UInt256
    /** The extended base hash, He.  */
    fun extendedBaseHash(): UInt256?
    /** Joint election key, K in the spec.  */
    fun jointPublicKey(): ElementModP?
    /** public data of the guardians. */
    fun guardians(): List<Guardian> // may be empty
    fun electionInit(): ElectionInitialized?

    fun encryptingDevices(): List<String>
    fun readEncryptedBallotChain(device: String) : Result<EncryptedBallotChain, ErrorMessages>
    fun encryptedBallots(device: String, filter : ((EncryptedBallot) -> Boolean)?): Iterable<EncryptedBallot> // may be empty
    fun encryptedAllBallots(filter : ((EncryptedBallot) -> Boolean)?): Iterable<EncryptedBallot> // may be empty

    fun encryptedTally(): EncryptedTally?
    fun tallyResult(): TallyResult?

    fun decryptedTally(): DecryptedTallyOrBallot?
    // fun decryptingGuardians(): List<LagrangeCoordinate> // may be empty
    fun decryptedBallots(): Iterable<DecryptedTallyOrBallot> // may be empty
    fun decryptionResult(): DecryptionResult?
}