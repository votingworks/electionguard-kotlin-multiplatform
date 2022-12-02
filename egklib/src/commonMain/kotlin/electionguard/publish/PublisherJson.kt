package electionguard.publish

import electionguard.ballot.*
import electionguard.keyceremony.KeyCeremonyTrustee

/** Read/write the Election Record as protobuf files. */
expect class PublisherJson(topDir: String, createNew: Boolean = false) : Publisher {
    override fun writeElectionConfig(config: ElectionConfig)
    override fun writeElectionInitialized(init: ElectionInitialized)
    override fun writeEncryptions(init: ElectionInitialized, ballots: Iterable<EncryptedBallot>)
    override fun writeTallyResult(tally: TallyResult)
    override fun writeDecryptionResult(decryption: DecryptionResult)

    override fun encryptedBallotSink(): EncryptedBallotSinkIF
    override fun decryptedTallyOrBallotSink(): DecryptedTallyOrBallotSinkIF

    override fun writePlaintextBallot(outputDir: String, plaintextBallots: List<PlaintextBallot>)
    override fun writeTrustee(trusteeDir: String, trustee: KeyCeremonyTrustee)
}
