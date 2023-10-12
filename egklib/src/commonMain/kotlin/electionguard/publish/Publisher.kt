package electionguard.publish

import electionguard.ballot.*
import electionguard.pep.BallotPep
import electionguard.keyceremony.KeyCeremonyTrustee

/** Write the Election Record as protobuf or json files. */
interface Publisher {
    fun isJson() : Boolean

    fun writeManifest(manifest: Manifest) : String // return filename
    fun writeElectionConfig(config: ElectionConfig)
    fun writeElectionInitialized(init: ElectionInitialized)
    fun writeTallyResult(tally: TallyResult)
    fun writeDecryptionResult(decryption: DecryptionResult)

    fun encryptedBallotSink(device: String, batched: Boolean = false): EncryptedBallotSinkIF
    fun writeEncryptedBallotChain(closing: EncryptedBallotChain)

    fun decryptedTallyOrBallotSink(): DecryptedTallyOrBallotSinkIF
    fun pepBallotSink(outputDir: String): PepBallotSinkIF

    fun writePlaintextBallot(outputDir: String, plaintextBallots: List<PlaintextBallot>)
    fun writeTrustee(trusteeDir: String, trustee: KeyCeremonyTrustee)
}

interface EncryptedBallotSinkIF : Closeable {
    fun writeEncryptedBallot(ballot: EncryptedBallot)
}

interface DecryptedTallyOrBallotSinkIF : Closeable {
    fun writeDecryptedTallyOrBallot(tally: DecryptedTallyOrBallot)
}

interface PepBallotSinkIF : Closeable {
    fun writePepBallot(pepBallot: BallotPep)
}

// copied from io.ktor.utils.io.core.Closeable to break package dependency
interface Closeable {
    fun close()
}

fun makePublisher(
    topDir: String,
    createNew: Boolean = false, // false = create directories if not already exist, true = create clean directories,
    jsonSerialization: Boolean = false, // false = protobuf, true = json
): Publisher {
    return if (jsonSerialization) PublisherJson(topDir, createNew) else PublisherProto(topDir, createNew)
}
