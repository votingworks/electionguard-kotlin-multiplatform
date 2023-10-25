package electionguard.publish

import electionguard.ballot.*
import electionguard.core.UInt256
import electionguard.json2.publishJson
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.pep.BallotPep
import electionguard.pep.publishJson
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
    val jsonIgnoreNulls = Json { explicitNulls = false }

    init {
        val electionRecordDir = Path.of(topDir)
        if (createNew) {
            removeAllFiles(electionRecordDir)
        }
        validateOutputDir(electionRecordDir, Formatter())
    }

    actual override fun isJson() : Boolean = true

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
            jsonIgnoreNulls.encodeToStream(plaintextBallotJson, out)
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

    ////////////////////////////////////////////////

    actual override fun writeEncryptedBallotChain(closing: EncryptedBallotChain) {
        val jsonChain = closing.publishJson()
        val filename = jsonPaths.encryptedBallotChain(closing.encryptingDevice)
        FileOutputStream(filename).use { out ->
            jsonFormat.encodeToStream(jsonChain, out)
            out.close()
        }
    }

    actual override fun encryptedBallotSink(device: String, batched: Boolean): EncryptedBallotSinkIF {
        val ballotDir = jsonPaths.encryptedBallotDir(device)
        validateOutputDir(Path.of(ballotDir), Formatter())
        return EncryptedBallotDeviceSink(device)
    }

    inner class EncryptedBallotDeviceSink(val device: String) : EncryptedBallotSinkIF {

        override fun writeEncryptedBallot(ballot: EncryptedBallot) {
            val ballotFile = jsonPaths.encryptedBallotDevicePath(device, ballot.ballotId)
            val json = ballot.publishJson()
            FileOutputStream(ballotFile).use { out ->
                jsonFormat.encodeToStream(json, out)
                out.close()
            }
        }
        override fun close() {
        }
    }

    /////////////////////////////////////////////////////////////

    actual override fun decryptedTallyOrBallotSink(): DecryptedTallyOrBallotSinkIF {
        validateOutputDir(Path.of(jsonPaths.decryptedBallotDir()), Formatter())
        return DecryptedTallyOrBallotSink()
    }

    inner class DecryptedTallyOrBallotSink : DecryptedTallyOrBallotSinkIF {
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

    ///////////////////////////////////////////////////////////////////////
    actual override fun pepBallotSink(outputDir: String): PepBallotSinkIF = PepBallotSink(outputDir)

    inner class PepBallotSink(val outputDir: String) : PepBallotSinkIF {
        override fun writePepBallot(pepBallot: BallotPep) {
            val pepJson = pepBallot.publishJson()
            FileOutputStream(jsonPaths.pepBallotPath(outputDir, pepBallot.ballotId)).use { out ->
                jsonFormat.encodeToStream(pepJson, out)
                out.close()
            }
        }
        override fun close() {
        }
    }


}