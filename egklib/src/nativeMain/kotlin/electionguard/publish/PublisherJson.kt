package electionguard.publish

import electionguard.ballot.*
import electionguard.core.pathExists
import electionguard.json2.*
import electionguard.keyceremony.KeyCeremonyTrustee
import kotlinx.cinterop.CPointer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.posix.FILE
import platform.posix.fclose

/** Write the Election Record as JSON files.  */
actual class PublisherJson actual constructor(topDir: String, createNew: Boolean) : Publisher {
    private var jsonPaths: ElectionRecordJsonPaths = ElectionRecordJsonPaths(topDir)
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }

    init {
        if (createNew) {
            removeAllFiles(topDir)
        }
        validateOutputDir(topDir)
    }

    actual override fun isJson() : Boolean = true

    private fun writeToFile(fileout: String, stringOut: String) {
        val file: CPointer<FILE> = openFile(fileout, "wb")
        try {
            writeToFile(file, fileout, stringOut.encodeToByteArray())
        } finally {
            fclose(file)
        }
    }

    private fun writeToFile(fileout: String, bytes: ByteArray) {
        val file: CPointer<FILE> = openFile(fileout, "wb")
        try {
            writeToFile(file, fileout, bytes)
        } finally {
            fclose(file)
        }
    }

    actual override fun writeManifest(manifest: Manifest) : String {
        val fileout2 = jsonPaths.manifestPath()
        val jsonString2 = jsonReader.encodeToString(manifest.publishJson())
        writeToFile(fileout2, jsonString2)
        return fileout2
    }

    actual override fun writeElectionConfig(config: ElectionConfig) {
        writeToFile(jsonPaths.electionConstantsPath(), jsonReader.encodeToString(config.constants.publishJson()))
        writeToFile(jsonPaths.electionConfigPath(), config.manifestBytes)
        writeToFile(jsonPaths.electionConfigPath(), jsonReader.encodeToString(config.publishJson()))
    }

    actual override fun writeElectionInitialized(init: ElectionInitialized) {
        writeElectionConfig(init.config)

        val fileout = jsonPaths.electionInitializedPath()
        val jsonString = jsonReader.encodeToString(init.publishJson())
        writeToFile(fileout, jsonString)
    }

    actual override fun writeTallyResult(tally: TallyResult) {
        writeElectionInitialized(tally.electionInitialized)

        val fileout = jsonPaths.encryptedTallyPath()
        val jsonString = jsonReader.encodeToString(tally.encryptedTally.publishJson())
        writeToFile(fileout, jsonString)
    }

    actual override fun writeDecryptionResult(decryption: DecryptionResult) {
        writeTallyResult(decryption.tallyResult)

        val fileout2 = jsonPaths.decryptedTallyPath()
        val jsonString2 = jsonReader.encodeToString(decryption.decryptedTally.publishJson())
        writeToFile(fileout2, jsonString2)
    }

    actual override fun writePlaintextBallot(outputDir: String, plaintextBallots: List<PlaintextBallot>) {
        plaintextBallots.forEach { writePlaintextBallot(outputDir, it) }
    }

    private fun writePlaintextBallot(outputDir: String, plaintextBallot: PlaintextBallot) {
        val fileout = jsonPaths.plaintextBallotPath(outputDir, plaintextBallot.ballotId)
        val jsonString = jsonReader.encodeToString(plaintextBallot.publishJson())
        writeToFile(fileout, jsonString)
    }

    actual override fun writeTrustee(trusteeDir: String, trustee: KeyCeremonyTrustee) {
        val fileout = jsonPaths.decryptingTrusteePath(trusteeDir, trustee.id)
        val jsonString = jsonReader.encodeToString(trustee.publishJson())
        writeToFile(fileout, jsonString)
    }

    actual override fun writeEncryptedBallotChain(closing: EncryptedBallotChain) {}

    actual override fun encryptedBallotSink(device: String, batched: Boolean): EncryptedBallotSinkIF {
        validateOutputDir(jsonPaths.encryptedBallotDir())
        return EncryptedBallotSink()
    }

    private inner class EncryptedBallotSink : EncryptedBallotSinkIF {

        override fun writeEncryptedBallot(ballot: EncryptedBallot) {
            val fileout = jsonPaths.encryptedBallotPath(ballot.ballotId)
            val jsonBallot = ballot.publishJson()
            val jsonString = jsonReader.encodeToString(jsonBallot)
            writeToFile(fileout, jsonString)
        }

        override fun close() {
        }
    }

    actual override fun decryptedTallyOrBallotSink(): DecryptedTallyOrBallotSinkIF {
        validateOutputDir(jsonPaths.decryptedBallotDir())
        return DecryptedTallyOrBallotSink()
    }

    private inner class DecryptedTallyOrBallotSink : DecryptedTallyOrBallotSinkIF {
        override fun writeDecryptedTallyOrBallot(tally: DecryptedTallyOrBallot) {
            val fileout = jsonPaths.decryptedBallotPath(tally.id)
            val jsonString = jsonReader.encodeToString(tally.publishJson())
            writeToFile(fileout, jsonString)
        }

        override fun close() {
        }
    }
}

/** Delete everything in the given directory, but leave that directory.  */
fun removeAllFiles(path: String) {
    if (!pathExists(path)) {
        return
    }
    // TODO not done
}

/** Make sure output dir exists and is writeable.  */
fun validateOutputDir(path: String): Boolean {
    if (!exists(path)) {
        createDirectories(path)
    }
    return true
}