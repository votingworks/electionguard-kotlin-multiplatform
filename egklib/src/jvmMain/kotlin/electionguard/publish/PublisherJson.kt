package electionguard.publish

import electionguard.ballot.*
import electionguard.json2.publishJson
import electionguard.keyceremony.KeyCeremonyTrustee
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.*

/** Publishes the Election Record to JSON files.  */
@OptIn(ExperimentalSerializationApi::class)
actual class PublisherJson actual constructor(topDir: String, createNew: Boolean) : Publisher {
    private var jsonPaths: ElectionRecordJsonPaths = ElectionRecordJsonPaths(topDir)
    private val jsonFormat = Json { prettyPrint = true }

    init {
        val electionRecordDir = Path.of(topDir)
        if (createNew) {
            removeAllFiles(electionRecordDir)
        }
        validateOutputDir(electionRecordDir, Formatter())
    }

    actual override fun writeManifest(manifest: Manifest)  : String {
        val manifestJson = manifest.publishJson()
        FileOutputStream(jsonPaths.manifestPath()).use { out ->
            jsonFormat.encodeToStream(manifestJson, out)
            out.close()
        }
        return jsonPaths.manifestPath()
    }

    actual override fun writeElectionConfig(config: ElectionConfig) {
        val constantsJson = config.constants.publishJson()
        FileOutputStream(jsonPaths.electionConstantsPath()).use { out ->
            jsonFormat.encodeToStream(constantsJson, out)
            out.close()
        }

        FileOutputStream(jsonPaths.manifestPath()).use { out ->
            out.write(config.manifestBytes)
            out.close()
        }

        val configJson = config.publishJson()
        FileOutputStream(jsonPaths.electionConfigPath()).use { out ->
            jsonFormat.encodeToStream(configJson, out)
            out.close()
        }
    }

    actual override fun writeElectionInitialized(init: ElectionInitialized) {
        writeElectionConfig(init.config)

        val contextJson = init.publishJson()
        FileOutputStream(jsonPaths.electionInitializedPath()).use { out ->
            jsonFormat.encodeToStream(contextJson, out)
            out.close()
        }
    }

    actual override fun writeEncryptions(init: ElectionInitialized, ballots: Iterable<EncryptedBallot>) {
        writeElectionInitialized(init)

        validateOutputDir(Path.of(jsonPaths.encryptedBallotDir()), Formatter())
        encryptedBallotSink().use { sink ->
            ballots.forEach { sink.writeEncryptedBallot(it) }
        }
    }

    actual override fun writeTallyResult(tally: TallyResult) {
        writeElectionInitialized(tally.electionInitialized)

        val encryptedTallyJson = tally.encryptedTally.publishJson()
        FileOutputStream(jsonPaths.encryptedTallyPath()).use { out ->
            jsonFormat.encodeToStream(encryptedTallyJson, out)
            out.close()
        }
    }

    actual override fun writeDecryptionResult(decryption: DecryptionResult) {
        writeTallyResult(decryption.tallyResult)

        val decryptedTallyJson = decryption.decryptedTally.publishJson()
        FileOutputStream(jsonPaths.decryptedTallyPath()).use { out ->
            jsonFormat.encodeToStream(decryptedTallyJson, out)
            out.close()
        }
    }

    actual override fun writePlaintextBallot(outputDir: String, plaintextBallots: List<PlaintextBallot>) {
        plaintextBallots.forEach { writePlaintextBallot(outputDir, it) }
    }

    private fun writePlaintextBallot(outputDir: String, plaintextBallot: PlaintextBallot) {
        val plaintextBallotJson = plaintextBallot.publishJson()
        FileOutputStream(jsonPaths.plaintextBallotPath(outputDir, plaintextBallot.ballotId)).use { out ->
            jsonFormat.encodeToStream(plaintextBallotJson, out)
            out.close()
        }
    }

    actual override fun writeTrustee(trusteeDir: String, trustee: KeyCeremonyTrustee) {
        val decryptingTrusteeJson = trustee.publishJson()
        FileOutputStream(jsonPaths.decryptingTrusteePath(trusteeDir, trustee.id)).use { out ->
            jsonFormat.encodeToStream(decryptingTrusteeJson, out)
            out.close()
        }
    }

    actual override fun writeEncryptedBallotChain(closing: EncryptedBallotChain) {}

    actual override fun encryptedBallotSink(): EncryptedBallotSinkIF {
        validateOutputDir(Path.of(jsonPaths.encryptedBallotDir()), Formatter())
        return EncryptedBallotSink()
    }

    actual override fun encryptedBallotSink(device: String): EncryptedBallotSinkIF {
        validateOutputDir(Path.of(jsonPaths.encryptedBallotDir()), Formatter())
        return EncryptedBallotSink()
    }

    inner class EncryptedBallotSink() : EncryptedBallotSinkIF {
        override fun writeEncryptedBallot(ballot: EncryptedBallot) {
            val ballotJson = ballot.publishJson()
            FileOutputStream(jsonPaths.encryptedBallotPath(ballot.ballotId)).use { out ->
                jsonFormat.encodeToStream(ballotJson, out)
                out.close()
            }
        }
        override fun close() {
        }
    }

    actual override fun decryptedTallyOrBallotSink(): DecryptedTallyOrBallotSinkIF {
        validateOutputDir(Path.of(jsonPaths.decryptedBallotDir()), Formatter())
        return DecryptedTallyOrBallotSink()
    }

    inner class DecryptedTallyOrBallotSink() : DecryptedTallyOrBallotSinkIF {
        override fun writeDecryptedTallyOrBallot(tally: DecryptedTallyOrBallot) {
            val tallyJson = tally.publishJson()
            FileOutputStream(jsonPaths.decryptedBallotPath(tally.id)).use { out ->
                jsonFormat.encodeToStream(tallyJson, out)
                out.close()
            }
        }
        override fun close() {
        }
    }
}