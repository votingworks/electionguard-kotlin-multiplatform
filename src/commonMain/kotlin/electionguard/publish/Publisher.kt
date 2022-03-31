package electionguard.publish

import electionguard.ballot.*

/** Read/write the Election Record as protobuf files. */
expect class Publisher(topDir: String, publisherMode: PublisherMode) {
    /** Publishes the election record. */
    fun writeElectionRecordProto(
        manifest: Manifest,
        constants: ElectionConstants,
        context: ElectionContext?,
        guardianRecords: List<GuardianRecord>?,
        devices: Iterable<EncryptionDevice>?,
        submittedBallots: Iterable<SubmittedBallot>?,
        ciphertextTally: CiphertextTally?,
        decryptedTally: PlaintextTally?,
        spoiledBallots: Iterable<PlaintextTally>?,
        availableGuardians: List<AvailableGuardian>?
    )
}

enum class PublisherMode {
    readonly, // read files only
    writeonly, // write new files, but do not create directories
    createIfMissing, // create directories if not already exist
    createNew // create clean directories
}
