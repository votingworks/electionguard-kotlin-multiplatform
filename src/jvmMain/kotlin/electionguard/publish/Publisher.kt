package electionguard.publish

import electionguard.ballot.*
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.protoconvert.publishDecryptingTrustee
import electionguard.protoconvert.publishDecryptionResult
import electionguard.protoconvert.publishElectionConfig
import electionguard.protoconvert.publishElectionInitialized
import electionguard.protoconvert.publishPlaintextBallot
import electionguard.protoconvert.publishPlaintextTally
import electionguard.protoconvert.publishEncryptedBallot
import electionguard.protoconvert.publishTallyResult
import electionguard.publish.ElectionRecordPath.Companion.DECRYPTION_RESULT_NAME
import electionguard.publish.ElectionRecordPath.Companion.ELECTION_CONFIG_FILE_NAME
import electionguard.publish.ElectionRecordPath.Companion.ELECTION_INITIALIZED_FILE_NAME
import electionguard.publish.ElectionRecordPath.Companion.SPOILED_BALLOT_FILE
import electionguard.publish.ElectionRecordPath.Companion.ENCRYPTED_BALLOT_PROTO
import electionguard.publish.ElectionRecordPath.Companion.TALLY_RESULT_NAME
import pbandk.encodeToStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path

/** Publishes the Manifest Record to Json or protobuf files.  */
actual class Publisher actual constructor(topDir: String, publisherMode: PublisherMode) {
    private val createPublisherMode: PublisherMode = publisherMode
    private val electionRecordDir = Path.of(topDir)
    private var path: ElectionRecordPath = ElectionRecordPath(topDir)

    init {
        if (createPublisherMode == PublisherMode.createNew) {
            if (!Files.exists(electionRecordDir)) {
                Files.createDirectories(electionRecordDir)
            } else {
                removeAllFiles(electionRecordDir)
            }
        } else if (createPublisherMode == PublisherMode.createIfMissing) {
            if (!Files.exists(electionRecordDir)) {
                Files.createDirectories(electionRecordDir)
            }
        } else {
            check(Files.exists(electionRecordDir)) { "Non existing election directory $electionRecordDir" }
        }
    }

    /** Delete everything in the given directory, but leave that directory.  */
    private fun removeAllFiles(path: Path) {
        if (!path.toFile().exists()) {
            return
        }
        Files.walk(path)
            .filter { p: Path -> p != path }
            .map { obj: Path -> obj.toFile() }
            .sorted { o1: File, o2: File? -> -o1.compareTo(o2) }
            .forEach { f: File -> f.delete() }
    }


    /** Make sure output dir exists and is writeable.  */
    fun validateOutputDir(error: java.util.Formatter): Boolean {
        if (!Files.exists(electionRecordDir)) {
            error.format(" Output directory '%s' does not exist%n", electionRecordDir)
            return false
        }
        if (!Files.isDirectory(electionRecordDir)) {
            error.format(" Output directory '%s' is not a directory%n", electionRecordDir)
            return false
        }
        if (!Files.isWritable(electionRecordDir)) {
            error.format(" Output directory '%s' is not writeable%n", electionRecordDir)
            return false
        }
        if (!Files.isExecutable(electionRecordDir)) {
            error.format(" Output directory '%s' is not executable%n", electionRecordDir)
            return false
        }
        return true
    }

    ////////////////////
    // duplicated from ElectionRecordPath so that we can use java.nio.file.Path

    fun electionConfigPath(): Path {
        return electionRecordDir.resolve(ELECTION_CONFIG_FILE_NAME).toAbsolutePath()
    }

    fun electionInitializedPath(): Path {
        return electionRecordDir.resolve(ELECTION_INITIALIZED_FILE_NAME).toAbsolutePath()
    }

    fun decryptionResultPath(): Path {
        return electionRecordDir.resolve(DECRYPTION_RESULT_NAME).toAbsolutePath()
    }

    fun spoiledBallotPath(): Path {
        return electionRecordDir.resolve(SPOILED_BALLOT_FILE).toAbsolutePath()
    }

    fun encryptedBallotPath(): Path {
        return electionRecordDir.resolve(ENCRYPTED_BALLOT_PROTO).toAbsolutePath()
    }

    fun tallyResultPath(): Path {
        return electionRecordDir.resolve(TALLY_RESULT_NAME).toAbsolutePath()
    }

    actual fun writeElectionConfig(config: ElectionConfig) {
        val proto = config.publishElectionConfig()
        FileOutputStream(electionConfigPath().toFile()).use { out ->
            proto.encodeToStream(out)
            out.close()
        }
    }

    actual fun writeElectionInitialized(init: ElectionInitialized) {
        val proto = init.publishElectionInitialized()
        FileOutputStream(electionInitializedPath().toFile()).use { out ->
            proto.encodeToStream(out)
            out.close()
        }
    }

    actual fun writeEncryptions(
        init: ElectionInitialized,
        ballots: Iterable<EncryptedBallot>
    ) {
        writeElectionInitialized(init)
        val sink = encryptedBallotSink()
        ballots.forEach {sink.writeEncryptedBallot(it) }
        sink.close()
    }

    actual fun writeTallyResult(tally: TallyResult) {
        val proto = tally.publishTallyResult()
        FileOutputStream(tallyResultPath().toFile()).use { out ->
            proto.encodeToStream(out)
            out.close()
        }
    }

    actual fun writeDecryptionResult(decryption: DecryptionResult) {
        val proto = decryption.publishDecryptionResult()
        FileOutputStream(decryptionResultPath().toFile()).use { out ->
            proto.encodeToStream(out)
            out.close()
        }
    }

    actual fun writePlaintextBallot(outputDir: String, plaintextBallots: List<PlaintextBallot>) {
        if (!plaintextBallots.isEmpty()) {
            val fileout = path.plaintextBallotPath(outputDir)
            FileOutputStream(fileout).use { out ->
                for (ballot in plaintextBallots) {
                    val ballotProto = ballot.publishPlaintextBallot()
                    writeDelimitedTo(ballotProto, out)
                }
                out.close()
            }
        }
    }

    actual fun writeTrustee(trusteeDir: String, trustee: KeyCeremonyTrustee) {
        val proto = trustee.publishDecryptingTrustee()
        val fileout = path.decryptingTrusteePath(trusteeDir, trustee.id)
        FileOutputStream(fileout).use { out ->
            proto.encodeToStream(out)
            out.close()
        }
    }

    actual fun encryptedBallotSink(): EncryptedBallotSinkIF =
        EncryptedBallotSink(encryptedBallotPath().toString())

    inner class EncryptedBallotSink(path: String) : EncryptedBallotSinkIF {
        val out: FileOutputStream = FileOutputStream(path)

        override fun writeEncryptedBallot(ballot: EncryptedBallot) {
            val ballotProto: pbandk.Message = ballot.publishEncryptedBallot()
            writeDelimitedTo(ballotProto, out)
        }

        override fun close() {
            out.close()
        }
    }

    actual fun plaintextTallySink(): PlaintextTallySinkIF =
        PlaintextTallySink(spoiledBallotPath().toString())

    inner class PlaintextTallySink(path: String) : PlaintextTallySinkIF {
        val out: FileOutputStream = FileOutputStream(path)

        override fun writePlaintextTally(tally: PlaintextTally){
            val ballotProto: pbandk.Message = tally.publishPlaintextTally()
            writeDelimitedTo(ballotProto, out)
        }

        override fun close() {
            out.close()
        }
    }

    fun writeDelimitedTo(proto: pbandk.Message, output: OutputStream) {
        val bb = ByteArrayOutputStream()
        proto.encodeToStream(bb)
        writeVlen(bb.size(), output)
        output.write(bb.toByteArray())
    }

    fun writeVlen(input: Int, output: OutputStream) {
        var value = input
        while (true) {
            if (value and 0x7F.inv() == 0) {
                output.write(value)
                return
            } else {
                output.write(value and 0x7F or 0x80)
                value = value ushr 7
            }
        }
    }
}