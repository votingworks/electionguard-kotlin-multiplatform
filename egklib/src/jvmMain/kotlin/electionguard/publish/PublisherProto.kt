package electionguard.publish

import electionguard.ballot.*
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.protoconvert.publishDecryptingTrusteeProto
import electionguard.protoconvert.publishProto
import electionguard.publish.ElectionRecordProtoPaths.Companion.DECRYPTION_RESULT_FILE
import electionguard.publish.ElectionRecordProtoPaths.Companion.ELECTION_CONFIG_FILE
import electionguard.publish.ElectionRecordProtoPaths.Companion.ELECTION_INITIALIZED_FILE
import electionguard.publish.ElectionRecordProtoPaths.Companion.SPOILED_BALLOT_FILE
import electionguard.publish.ElectionRecordProtoPaths.Companion.ENCRYPTED_BALLOT_FILE
import electionguard.publish.ElectionRecordProtoPaths.Companion.MANIFEST_FILE
import electionguard.publish.ElectionRecordProtoPaths.Companion.TALLY_RESULT_FILE
import pbandk.encodeToStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

/** Publishes the Manifest Record to protobuf files.  */
actual class PublisherProto actual constructor(topDir: String, createNew: Boolean) : Publisher {
    private val electionRecordDir = Path.of(topDir)
    private var path: ElectionRecordProtoPaths = ElectionRecordProtoPaths(topDir)

    init {
        if (createNew) {
            if (!Files.exists(electionRecordDir)) {
                Files.createDirectories(electionRecordDir)
            } else {
                removeAllFiles(electionRecordDir)
            }
        } else {
            if (!Files.exists(electionRecordDir)) {
                Files.createDirectories(electionRecordDir)
            }
        }
        validateOutputDir(electionRecordDir, Formatter())
    }

    ////////////////////
    // duplicated from ElectionRecordPath so that we can use java.nio.file.Path

    fun manifestPath(): Path {
        return electionRecordDir.resolve(MANIFEST_FILE).toAbsolutePath()
    }

    fun electionConfigPath(): Path {
        return electionRecordDir.resolve(ELECTION_CONFIG_FILE).toAbsolutePath()
    }

    fun electionInitializedPath(): Path {
        return electionRecordDir.resolve(ELECTION_INITIALIZED_FILE).toAbsolutePath()
    }

    fun decryptionResultPath(): Path {
        return electionRecordDir.resolve(DECRYPTION_RESULT_FILE).toAbsolutePath()
    }

    fun spoiledBallotPath(): Path {
        return electionRecordDir.resolve(SPOILED_BALLOT_FILE).toAbsolutePath()
    }

    fun encryptedBallotPath(): Path {
        return electionRecordDir.resolve(ENCRYPTED_BALLOT_FILE).toAbsolutePath()
    }

    fun tallyResultPath(): Path {
        return electionRecordDir.resolve(TALLY_RESULT_FILE).toAbsolutePath()
    }

    actual override fun writeManifest(manifest: Manifest) {
        val proto = manifest.publishProto()
        FileOutputStream(manifestPath().toFile()).use { out ->
            proto.encodeToStream(out)
            out.close()
        }
    }

    actual override fun writeElectionConfig(config: ElectionConfig) {
        val proto = config.publishProto()
        FileOutputStream(electionConfigPath().toFile()).use { out ->
            proto.encodeToStream(out)
            out.close()
        }
    }

    actual override fun writeElectionInitialized(init: ElectionInitialized) {
        val proto = init.publishProto()
        FileOutputStream(electionInitializedPath().toFile()).use { out ->
            proto.encodeToStream(out)
            out.close()
        }
    }

    actual override fun writeEncryptions(
        init: ElectionInitialized,
        ballots: Iterable<EncryptedBallot>
    ) {
        writeElectionInitialized(init)
        encryptedBallotSink().use { sink ->
            ballots.forEach { sink.writeEncryptedBallot(it) }
        }
    }

    actual override fun writeTallyResult(tally: TallyResult) {
        val proto = tally.publishProto()
        FileOutputStream(tallyResultPath().toFile()).use { out ->
            proto.encodeToStream(out)
            out.close()
        }
    }

    actual override fun writeDecryptionResult(decryption: DecryptionResult) {
        val proto = decryption.publishProto()
        FileOutputStream(decryptionResultPath().toFile()).use { out ->
            proto.encodeToStream(out)
            out.close()
        }
    }

    actual override fun writePlaintextBallot(outputDir: String, plaintextBallots: List<PlaintextBallot>) {
        if (plaintextBallots.isNotEmpty()) {
            val fileout = path.plaintextBallotPath(outputDir)
            FileOutputStream(fileout).use { out ->
                for (ballot in plaintextBallots) {
                    val ballotProto = ballot.publishProto()
                    writeDelimitedTo(ballotProto, out)
                }
                out.close()
            }
        }
    }

    actual override fun writeTrustee(trusteeDir: String, trustee: KeyCeremonyTrustee) {
        val proto = trustee.publishDecryptingTrusteeProto()
        val fileout = path.decryptingTrusteePath(trusteeDir, trustee.id)
        FileOutputStream(fileout).use { out ->
            proto.encodeToStream(out)
            out.close()
        }
    }

    actual override fun encryptedBallotSink(): EncryptedBallotSinkIF =
        EncryptedBallotSink(encryptedBallotPath().toString())

    inner class EncryptedBallotSink(path: String) : EncryptedBallotSinkIF {
        val out: FileOutputStream = FileOutputStream(path)

        override fun writeEncryptedBallot(ballot: EncryptedBallot) {
            val ballotProto: pbandk.Message = ballot.publishProto()
            writeDelimitedTo(ballotProto, out)
        }

        override fun close() {
            out.close()
        }
    }

    actual override fun decryptedTallyOrBallotSink(): DecryptedTallyOrBallotSinkIF =
        DecryptedTallyOrBallotSink(spoiledBallotPath().toString())

    inner class DecryptedTallyOrBallotSink(path: String) : DecryptedTallyOrBallotSinkIF {
        val out: FileOutputStream = FileOutputStream(path)

        override fun writeDecryptedTallyOrBallot(tally: DecryptedTallyOrBallot){
            val ballotProto: pbandk.Message = tally.publishProto()
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

/** Delete everything in the given directory, but leave that directory.  */
fun removeAllFiles(path: Path) {
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
fun validateOutputDir(path: Path, error: java.util.Formatter): Boolean {
    if (!Files.exists(path)) {
        Files.createDirectories(path)
    }
    if (!Files.isDirectory(path)) {
        error.format(" Output directory '%s' is not a directory%n", path)
        return false
    }
    if (!Files.isWritable(path)) {
        error.format(" Output directory '%s' is not writeable%n", path)
        return false
    }
    if (!Files.isExecutable(path)) {
        error.format(" Output directory '%s' is not executable%n", path)
        return false
    }
    return true
}