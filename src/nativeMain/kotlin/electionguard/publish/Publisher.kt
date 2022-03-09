package electionguard.publish

import electionguard.ballot.*

/** Read/write the Election Record as protobuf files.  */
actual class Publisher actual constructor(where: String, publisherMode: PublisherMode) {

    /** Publishes the entire election record as proto.  */
    actual fun writeElectionRecordProto(
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
    ) {
        TODO("Not yet implemented")
    }

}