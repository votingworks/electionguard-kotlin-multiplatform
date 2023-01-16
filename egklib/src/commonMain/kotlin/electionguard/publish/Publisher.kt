package electionguard.publish

import electionguard.ballot.*
import electionguard.keyceremony.KeyCeremonyTrustee

/** Write the Election Record as protobuf or json files. */
interface Publisher {
    fun writeManifest(manifest: Manifest)
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

fun makePublisher(
    topDir: String,
    createNew: Boolean = false, // false = create directories if not already exist, true = create clean directories,
    jsonSerialization: Boolean = false, // false = protobuf, true = json
): Publisher {
    return if (jsonSerialization) PublisherJson(topDir, createNew) else PublisherProto(topDir, createNew)
}
