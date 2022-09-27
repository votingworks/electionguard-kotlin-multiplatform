package electionguard.publish

import electionguard.ballot.*
import electionguard.keyceremony.KeyCeremonyTrustee

/** Read/write the Election Record as protobuf files. */
expect class Publisher(topDir: String, publisherMode: PublisherMode) {
    fun writeElectionConfig(config: ElectionConfig)
    fun writeElectionInitialized(init: ElectionInitialized)
    fun writeEncryptions(init: ElectionInitialized, ballots: Iterable<EncryptedBallot>)
    fun writeTallyResult(tally: TallyResult)
    fun writeDecryptionResult(decryption: DecryptionResult)

    fun encryptedBallotSink(): EncryptedBallotSinkIF
    fun decryptedTallyOrBallotSink(): DecryptedTallyOrBallotSinkIF

    fun writePlaintextBallot(outputDir: String, plaintextBallots: List<PlaintextBallot>)
    fun writeTrustee(trusteeDir: String, trustee: KeyCeremonyTrustee)
}

interface EncryptedBallotSinkIF {
    fun writeEncryptedBallot(ballot: EncryptedBallot)
    fun close()
}

interface DecryptedTallyOrBallotSinkIF {
    fun writeDecryptedTallyOrBallot(tally: DecryptedTallyOrBallot)
    fun close()
}

enum class PublisherMode {
    readonly, // read files only
    writeonly, // write new files, but do not create directories
    createIfMissing, // create directories if not already exist
    createNew // create clean directories
}
