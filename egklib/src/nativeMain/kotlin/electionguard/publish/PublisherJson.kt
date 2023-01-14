package electionguard.publish

import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.DecryptionResult
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.Guardian
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.TallyResult
import electionguard.core.fileExists
import electionguard.json.publish
import electionguard.json.publishDecryptingTrusteeJson
import electionguard.keyceremony.KeyCeremonyTrustee
import kotlinx.cinterop.CPointer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.posix.FILE
import platform.posix.fclose
import platform.posix.fflush

/** Write the Election Record as protobuf files.  */
actual class PublisherJson actual constructor(topDir: String, createNew: Boolean) : Publisher {
    private var jsonPaths: ElectionRecordJsonPaths = ElectionRecordJsonPaths(topDir)
    private val jsonFormat = Json { prettyPrint = true }

    init {
        if (createNew) {
            removeAllFiles(topDir)
        }
        validateOutputDir(topDir)
    }

    private fun writeToFile(fileout: String, stringOut : String) {
        val file: CPointer<FILE> = openFile(fileout, "wb")
        try {
            writeToFile(file, fileout, stringOut.encodeToByteArray())
        } finally {
            fflush(file)
            fclose(file)
        }
    }

    actual override fun writeElectionConfig(config: ElectionConfig) {
        val fileout = jsonPaths.electionConstantsPath()
        val jsonString = jsonFormat.encodeToString(config.constants.publish())
        writeToFile(fileout, jsonString)

        val fileout2 = jsonPaths.manifestPath()
        val jsonString2 = jsonFormat.encodeToString(config.manifest.publish())
        writeToFile(fileout2, jsonString2)
    }

    actual override fun writeElectionInitialized(init: ElectionInitialized) {
        writeElectionConfig(init.config)

        validateOutputDir(jsonPaths.guardianDir())
        init.guardians.forEach { writeGuardian(it) }

        val fileout = jsonPaths.electionContextPath()
        val jsonString = jsonFormat.encodeToString(init.publish())
        writeToFile(fileout, jsonString)
    }

    private fun writeGuardian(guardian: Guardian) {
        val fileout = jsonPaths.guardianPath(guardian.guardianId)
        val jsonString = jsonFormat.encodeToString(guardian.publish())
        writeToFile(fileout, jsonString)
    }

    actual override fun writeEncryptions(init: ElectionInitialized, ballots: Iterable<EncryptedBallot>) {
        writeElectionInitialized(init)

        validateOutputDir(jsonPaths.encryptedBallotDir())
        val sink = encryptedBallotSink()
        ballots.forEach { sink.writeEncryptedBallot(it) }
        sink.close()
    }

    actual override fun writeTallyResult(tally: TallyResult) {
        writeElectionInitialized(tally.electionInitialized)

        val fileout = jsonPaths.encryptedTallyPath()
        val jsonString = jsonFormat.encodeToString(tally.encryptedTally.publish())
        writeToFile(fileout, jsonString)
    }

    actual override fun writeDecryptionResult(decryption: DecryptionResult) {
        writeTallyResult(decryption.tallyResult)

        // all the coefficients in a map in one file
        val fileout = jsonPaths.lagrangePath()
        val jsonString = jsonFormat.encodeToString(decryption.lagrangeCoordinates.publish())
        writeToFile(fileout, jsonString)

        val fileout2 = jsonPaths.decryptedTallyPath()
        val jsonString2 = jsonFormat.encodeToString(decryption.decryptedTally.publish())
        writeToFile(fileout2, jsonString2)
    }

    actual override fun writePlaintextBallot(outputDir: String, plaintextBallots: List<PlaintextBallot>) {
        plaintextBallots.forEach { writePlaintextBallot(outputDir, it) }
    }

    private fun writePlaintextBallot(outputDir: String, plaintextBallot: PlaintextBallot) {
        val fileout = jsonPaths.plaintextBallotPath(outputDir, plaintextBallot.ballotId)
        val jsonString = jsonFormat.encodeToString(plaintextBallot.publish())
        writeToFile(fileout, jsonString)
    }

    actual override fun writeTrustee(trusteeDir: String, trustee: KeyCeremonyTrustee) {
        val fileout = jsonPaths.decryptingTrusteePath(trusteeDir, trustee.id)
        val jsonString = jsonFormat.encodeToString(trustee.publishDecryptingTrusteeJson())
        writeToFile(fileout, jsonString)
    }

    actual override fun encryptedBallotSink(): EncryptedBallotSinkIF {
        validateOutputDir(jsonPaths.encryptedBallotDir())
        return EncryptedBallotSink()
    }

    private inner class EncryptedBallotSink : EncryptedBallotSinkIF {
        override fun writeEncryptedBallot(ballot: EncryptedBallot) {
            val fileout = jsonPaths.encryptedBallotPath(ballot.ballotId)
            val jsonString = jsonFormat.encodeToString(ballot.publish())
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
            val jsonString = jsonFormat.encodeToString(tally.publish())
            writeToFile(fileout, jsonString)
        }

        override fun close() {
        }
    }
}

/** Delete everything in the given directory, but leave that directory.  */
fun removeAllFiles(path: String) {
    if (!fileExists(path)) {
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