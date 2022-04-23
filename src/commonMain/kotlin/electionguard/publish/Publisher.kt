package electionguard.publish

import electionguard.ballot.*

/** Read/write the Election Record as protobuf files. */
expect class Publisher(topDir: String, publisherMode: PublisherMode) {
    fun writeElectionConfig(config: ElectionConfig)
    fun writeElectionInitialized(init: ElectionInitialized)
    fun writeEncryptions(init: ElectionInitialized, ballots: Iterable<SubmittedBallot>)
    fun writeTallyResult(tally: TallyResult)
    fun writeDecryptionResult(decryption: DecryptionResult)

    fun submittedBallotSink(): SubmittedBallotSinkIF

    fun writeInvalidBallots(invalidDir: String, invalidBallots: List<PlaintextBallot>)
}

interface SubmittedBallotSinkIF {
    fun writeSubmittedBallot(ballot: SubmittedBallot)
    fun close()
}

enum class PublisherMode {
    readonly, // read files only
    writeonly, // write new files, but do not create directories
    createIfMissing, // create directories if not already exist
    createNew // create clean directories
}
