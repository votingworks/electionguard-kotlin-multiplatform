package electionguard.publish

import electionguard.ballot.*
import electionguard.json.publish
import electionguard.json.publishDecryptingTrusteeJson
import electionguard.keyceremony.KeyCeremonyTrustee
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.*

/** Publishes the Manifest Record to protobuf files.  */
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

    actual override fun writeElectionConfig(config: ElectionConfig) {
        val constantsJson = config.constants.publish()
        FileOutputStream(jsonPaths.electionConstantsPath()).use { out ->
            jsonFormat.encodeToStream(constantsJson, out)
            out.close()
        }

        val manifestJson = config.manifest.publish()
        FileOutputStream(jsonPaths.manifestPath()).use { out ->
            jsonFormat.encodeToStream(manifestJson, out)
            out.close()
        }
    }

    actual override fun writeElectionInitialized(init: ElectionInitialized) {
        writeElectionConfig(init.config)

        validateOutputDir(Path.of(jsonPaths.guardianDir()), Formatter())
        init.guardians.forEach { writeGuardian(it) }

        val contextJson = init.publish()
        FileOutputStream(jsonPaths.electionContextPath()).use { out ->
            jsonFormat.encodeToStream(contextJson, out)
            out.close()
        }
    }

    private fun writeGuardian(guardian: Guardian) {
        val guardianJson = guardian.publish()
        FileOutputStream(jsonPaths.guardianPath(guardian.guardianId)).use { out ->
            jsonFormat.encodeToStream(guardianJson, out)
            out.close()
        }
    }

    actual override fun writeEncryptions(init: ElectionInitialized, ballots: Iterable<EncryptedBallot>) {
        writeElectionInitialized(init)

        validateOutputDir(Path.of(jsonPaths.encryptedBallotDir()), Formatter())
        val sink = encryptedBallotSink()
        ballots.forEach {sink.writeEncryptedBallot(it) }
        sink.close()
    }

    actual override fun writeTallyResult(tally: TallyResult) {
        val encryptedTallyJson = tally.encryptedTally.publish()
        FileOutputStream(jsonPaths.encryptedTallyPath()).use { out ->
            jsonFormat.encodeToStream(encryptedTallyJson, out)
            out.close()
        }
    }

    actual override fun writeDecryptionResult(decryption: DecryptionResult) {
        // all the coefficients in a map in one file
        val coefficientsJson = decryption.lagrangeCoordinates.publish()
        FileOutputStream(jsonPaths.lagrangePath()).use { out ->
            jsonFormat.encodeToStream(coefficientsJson, out)
            out.close()
        }

        val decryptedTallyJson = decryption.decryptedTally.publish()
        FileOutputStream(jsonPaths.decryptedTallyPath()).use { out ->
            jsonFormat.encodeToStream(decryptedTallyJson, out)
            out.close()
        }
    }

    actual override fun writePlaintextBallot(outputDir: String, plaintextBallots: List<PlaintextBallot>) {
        plaintextBallots.forEach { writePlaintextBallot(outputDir, it) }
    }

    private fun writePlaintextBallot(outputDir: String, plaintextBallot: PlaintextBallot) {
        val plaintextBallotJson = plaintextBallot.publish()
        FileOutputStream(jsonPaths.plaintextBallotPath(outputDir, plaintextBallot.ballotId)).use { out ->
            jsonFormat.encodeToStream(plaintextBallotJson, out)
            out.close()
        }
    }

    actual override fun writeTrustee(trusteeDir: String, trustee: KeyCeremonyTrustee) {
        val decryptingTrusteeJson = trustee.publishDecryptingTrusteeJson()
        FileOutputStream(jsonPaths.decryptingTrusteePath(trusteeDir, trustee.id)).use { out ->
            jsonFormat.encodeToStream(decryptingTrusteeJson, out)
            out.close()
        }
    }

    actual override fun encryptedBallotSink(): EncryptedBallotSinkIF {
        validateOutputDir(Path.of(jsonPaths.encryptedBallotDir()), Formatter())
        return EncryptedBallotSink()
    }

    inner class EncryptedBallotSink() : EncryptedBallotSinkIF {
        override fun writeEncryptedBallot(ballot: EncryptedBallot) {
            val ballotJson = ballot.publish()
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
            val tallyJson = tally.publish()
            FileOutputStream(jsonPaths.decryptedBallotPath(tally.id)).use { out ->
                jsonFormat.encodeToStream(tallyJson, out)
                out.close()
            }
        }
        override fun close() {
        }
    }
}