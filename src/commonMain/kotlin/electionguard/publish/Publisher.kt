package electionguard.publish

import electionguard.ballot.*

/** Read/write the Election Record as protobuf files.  */
expect class Publisher(where: String, publisherMode: PublisherMode) {

    /** Publishes the entire election record as proto.  */
    fun writeElectionRecordProto(
        manifest: Manifest,
        context: ElectionContext,
        constants: ElectionConstants,
        guardianRecords: List<GuardianRecord>,
        devices: Iterable<EncryptionDevice>,
        submittedBallots: Iterable<SubmittedBallot>?,
        ciphertextTally: CiphertextTally?,
        decryptedTally: PlaintextTally?,
        spoiledBallots: Iterable<PlaintextTally>?,
        availableGuardians: List<AvailableGuardian>?
    )
}

enum class PublisherMode {
    readonly,   // read files only
    writeonly,  // write new files, but do not create directories
    createIfMissing,  // create directories if not already exist
    createNew   // create clean directories
}

// expect classes cant have companion object, so this is PROTO_VERSION not Publisher.PROTO_VERSION
const val PROTO_VERSION = "1.0.0"

