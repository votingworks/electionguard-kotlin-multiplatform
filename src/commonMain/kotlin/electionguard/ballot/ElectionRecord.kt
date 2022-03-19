package electionguard.ballot

import electionguard.core.*

/**
 * The entire published election record, all data assumed present
 *
 * @param electionRecord the record of all the public election data (except submittedBallots and
 *     spoiledBallots)
 * @param submittedBallots all submitted ballots (CAST and SPOILED)
 * @param spoiledBallots decrypted spoiled ballots as PlaintextTally
 */
data class ElectionRecordAllData(
    val protoVersion: String,
    val constants: ElectionConstants,
    val manifest: Manifest,
    val context: ElectionContext,
    val guardianRecords: List<GuardianRecord>,
    val devices: List<EncryptionDevice>,
    val encryptedTally: CiphertextTally,
    val decryptedTally: PlaintextTally,
    val availableGuardians: List<AvailableGuardian>,
    val submittedBallots: Iterable<SubmittedBallot>,
    val spoiledBallots: Iterable<PlaintextTally>,
)

/**
 * The published election record, many fields are optional.
 */
data class ElectionRecord(
    val protoVersion: String,
    val constants: ElectionConstants,
    val manifest: Manifest,
    val context: ElectionContext?,
    val guardianRecords: List<GuardianRecord>?,
    val devices: List<EncryptionDevice>?,
    val encryptedTally: CiphertextTally?,
    val decryptedTally: PlaintextTally?,
    val availableGuardians: List<AvailableGuardian>?
)

/**
 * An available Guardian when decrypting.
 *
 * @param guardianId The guardian id
 * @param sequence the guardian x coordinate value
 * @param lagrangeCoordinate the lagrange coordinate when decrypting
 */
data class AvailableGuardian(
    var guardianId: String,
    var xCoordinate: Int,
    var lagrangeCoordinate: ElementModQ
)

/**
 * A public description of the mathematical group used for the encryption and processing of ballots.
 * One of these should accompany every batch of encrypted ballots, allowing future code that might
 * process those ballots to determine what parameters were in use and possibly give a warning or
 * error if they were unexpected.
 *
 * The byte arrays are defined to be big-endian.
 */
data class ElectionConstants(
    val name : String,
    /** large prime or P. */
    val largePrime: ByteArray,
    /** small prime or Q. */
    val smallPrime: ByteArray,
    /** cofactor or R. */
    val cofactor: ByteArray,
    /** generator or G. */
    val generator: ByteArray,
)

/**
 * The cryptographic context of an election.
 *
 * @see [Baseline Parameters](https://www.electionguard.vote/spec/0.95.0/3_Baseline_parameters/) for
 * definition of 𝑄.
 *
 * @see
 *     [Key Generation](https://www.electionguard.vote/spec/0.95.0/4_Key_generation/.details-of-key-generation)
 *     for defintion of K, 𝑄, 𝑄'.
 */
data class ElectionContext(
    /** The number of guardians necessary to generate the public key. */
    val numberOfGuardians: Int,
    /**
     * The quorum of guardians necessary to decrypt an election. Must be less than
     * number_of_guardians.
     */
    val quorum: Int,
    /** The joint public key (K) in the ElectionGuard Spec. */
    val jointPublicKey: ElementModP,
    val manifestHash: UInt256, // matches Manifest.cryptoHash
    val cryptoBaseHash: UInt256,
    val cryptoExtendedBaseHash: UInt256,
    val commitmentHash: UInt256,
    val extendedData: Map<String, String>?
)

data class EncryptionDevice(
    /** Unique identifier for device. */
    val deviceId: Long,
    /** Used to identify session and protect the timestamp. */
    val sessionId: Long,
    /** Election initialization value. */
    val launchCode: Long,
    val location: String,
)

/** Published record per Guardian used in verification processes. */
data class GuardianRecord(
    val guardianId: String,
    val xCoordinate: Int,
    val guardianPublicKey: ElementModP,
    val coefficientCommitments: List<ElementModP>,
    val coefficientProofs: List<SchnorrProof>
)