package electionguard.ballot

import electionguard.core.*

/** The published election record for a collection of ballots, eg from a single encryption device.  */
data class ElectionRecord(
    val protoVersion: String,
    val constants: ElectionConstants,
    val manifest: Manifest,
    val context: ElectionContext,
    val guardianRecords: List<GuardianRecord>?,
    val devices: List<EncryptionDevice>,
    val encryptedTally: CiphertextTally?,
    val decryptedTally: PlaintextTally?,
    val acceptedBallots: Iterable<SubmittedBallot>?,
    val spoiledBallots: Iterable<PlaintextTally>?,
    val availableGuardians: List<AvailableGuardian>?
)

/**
 * An available Guardian when decrypting.
 * @param guardian_id The guardian id
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
    /** large prime or P.  */
    val largePrime: ByteArray,
    /** small prime or Q. */
    val smallPrime: ByteArray,
    /** cofactor or R.  */
    val cofactor: ByteArray,
    /** generator or G.  */
    val generator: ByteArray,
)

/**
 * The cryptographic context of an election.
 * @see [Baseline Parameters](https://www.electionguard.vote/spec/0.95.0/3_Baseline_parameters/)
 * for definition of 𝑄.
 *
 * @see [Key Generation](https://www.electionguard.vote/spec/0.95.0/4_Key_generation/.details-of-key-generation)
 * for defintion of K, 𝑄, 𝑄'.
 */
data class ElectionContext(
    /** The number of guardians necessary to generate the public key.  */
    val numberOfGuardians: Int,
    /** The quorum of guardians necessary to decrypt an election.  Must be less than number_of_guardians.  */
    val quorum: Int,
    /** The joint public key (K) in the ElectionGuard Spec.  */
    val jointPublicKey: ElementModP,
    val manifestHash: ElementModQ,
    val cryptoBaseHash: ElementModQ,
    val cryptoExtendedBaseHash: ElementModQ,
    val commitmentHash: ElementModQ,
    val extendedData: Map<String, String>?
)

data class EncryptionDevice(
    /** Unique identifier for device.  */
    val deviceId: Long,
    /** Used to identify session and protect the timestamp.  */
    val sessionId: Long,
    /** Election initialization value.  */
    val launchCode: Long,
    val location: String,
)

/**
 * Published record per Guardian used in verification processes.
 */
data class GuardianRecord(
    val guardianId: String,
    val xCoordinate: Int,
    val electionPublicKey: ElementModP,
    val coefficientCommitments: List<ElementModP>,
    val coefficientProofs: List<SchnorrProof>
)


/////////////////////////////////////////////////
/**
 * Makes a CiphertextElectionContext object. python: election.make_ciphertext_election_context().
 * python: make_ciphertext_election_context()
 * @param number_of_guardians The number of guardians necessary to generate the public key.
 * @param quorum The quorum of guardians necessary to decrypt an election.  Must be less than number_of_guardians.
 * @param jointPublicKey the joint public key of the election, K.
 * @param manifest the election manifest.
 * @param commitment_hash all the public commitments for all the guardians = H(K 1,0 , K 1,1 , K 1,2 , ... ,
 * K 1,k−1 , K 2,0 , K 2,1 , K 2,2 , ... , K 2,k−1 , ... , K n,0 , K n,1 , K n,2 , ... , K n,k−1 )
 */
fun makeCiphertextElectionContext(
    groupContext: GroupContext,
    number_of_guardians: Int,
    quorum: Int,
    jointPublicKey: ElementModP,
    manifest: Manifest,
    commitment_hash: ElementModQ,
    extended_data: Map<String, String>?
): ElectionContext {

    val crypto_base_hash: ElementModQ = groupContext.hashElements(
        groupContext.constants.largePrime,
        groupContext.constants.smallPrime,
        groupContext.constants.generator,
        number_of_guardians, quorum, manifest.cryptoHashElement()
    )
    val crypto_extended_base_hash: ElementModQ = groupContext.hashElements(crypto_base_hash, commitment_hash)

    return ElectionContext(
        number_of_guardians,
        quorum,
        jointPublicKey,
        manifest.cryptoHashElement(),
        crypto_base_hash,
        crypto_extended_base_hash,
        commitment_hash,
        extended_data
    )
}