package electionguard.publish

import electionguard.ballot.*
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.pep.BallotPep
import electionguard.protoconvert.publishProto
import electionguard.publish.ElectionRecordProtoPaths.Companion.DECRYPTION_RESULT_FILE
import electionguard.publish.ElectionRecordProtoPaths.Companion.ELECTION_CONFIG_FILE
import electionguard.publish.ElectionRecordProtoPaths.Companion.ELECTION_INITIALIZED_FILE
import electionguard.publish.ElectionRecordProtoPaths.Companion.DECRYPTED_BATCH_FILE
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
    private val protoPaths: ElectionRecordProtoPaths = ElectionRecordProtoPaths(topDir)

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

    actual override fun isJson(): Boolean = false

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
        return electionRecordDir.resolve(DECRYPTED_BATCH_FILE).toAbsolutePath()
    }

    fun tallyResultPath(): Path {
        return electionRecordDir.resolve(TALLY_RESULT_FILE).toAbsolutePath()
    }

    actual override fun writeManifest(manifest: Manifest): String {
        val proto = manifest.publishProto()
        FileOutputStream(manifestPath().toFile()).use { out ->
            proto.encodeToStream(out)
        }
        return manifestPath().toString()
    }

    actual override fun writeElectionConfig(config: ElectionConfig) {
        val proto = config.publishProto()
        FileOutputStream(electionConfigPath().toFile()).use { out ->
            proto.encodeToStream(out)
        }
    }

    actual override fun writeElectionInitialized(init: ElectionInitialized) {
        val proto = init.publishProto()
        FileOutputStream(electionInitializedPath().toFile()).use { out ->
            proto.encodeToStream(out)
        }
    }

    actual override fun writeTallyResult(tally: TallyResult) {
        val proto = tally.publishProto()
        FileOutputStream(tallyResultPath().toFile()).use { out ->
            proto.encodeToStream(out)
        }
    }

    actual override fun writeDecryptionResult(decryption: DecryptionResult) {
        val proto = decryption.publishProto()
        FileOutputStream(decryptionResultPath().toFile()).use { out ->
            proto.encodeToStream(out)
        }
    }

    actual override fun writePlaintextBallot(outputDir: String, plaintextBallots: List<PlaintextBallot>) {
        if (plaintextBallots.isNotEmpty()) {
            val fileout = protoPaths.plaintextBallotPath(outputDir)
            FileOutputStream(fileout).use { out ->
                for (ballot in plaintextBallots) {
                    val ballotProto = ballot.publishProto()
                    writeDelimitedTo(ballotProto, out)
                }
            }
        }
    }

    actual override fun writeTrustee(trusteeDir: String, trustee: KeyCeremonyTrustee) {
        val proto = trustee.publishProto()
        val fileout = protoPaths.decryptingTrusteePath(trusteeDir, trustee.id)
        FileOutputStream(fileout).use { out ->
            proto.encodeToStream(out)
        }
    }

    ////////////////////////////////////////////////

    actual override fun writeEncryptedBallotChain(closing: EncryptedBallotChain) {
        val proto = closing.publishProto()
        val filename = protoPaths.encryptedBallotChain(closing.encryptingDevice)

        FileOutputStream(filename).use { out ->
            proto.encodeToStream(out)
        }
    }

    actual override fun encryptedBallotSink(device: String, batched: Boolean): EncryptedBallotSinkIF {
        val ballotDir = protoPaths.encryptedBallotDir(device)
        validateOutputDir(Path.of(ballotDir), Formatter())
        return if (batched) EncryptedBallotBatchedSink(device, protoPaths.encryptedBallotBatched(device))
        else EncryptedBallotDeviceSink(device)
    }

    inner class EncryptedBallotDeviceSink(val device: String) : EncryptedBallotSinkIF {

        override fun writeEncryptedBallot(ballot: EncryptedBallot) {
            val ballotFile = protoPaths.encryptedBallotDevicePath(device, ballot.ballotId)
            val ballotProto: pbandk.Message = ballot.publishProto()
            FileOutputStream(ballotFile).use { out -> ballotProto.encodeToStream(out) }
        }

        override fun close() {
        }
    }

    inner class EncryptedBallotBatchedSink(val device: String, path: String) : EncryptedBallotSinkIF {
        val out: FileOutputStream = FileOutputStream(path, true) // append

        override fun writeEncryptedBallot(ballot: EncryptedBallot) {
            val ballotProto: pbandk.Message = ballot.publishProto()
            writeDelimitedTo(ballotProto, out)
        }

        override fun close() {
            out.close()
        }
    }

    /////////////////////////////////////////////////////////////

    actual override fun decryptedTallyOrBallotSink(): DecryptedTallyOrBallotSinkIF =
        DecryptedTallyOrBallotSink(spoiledBallotPath().toString())

    inner class DecryptedTallyOrBallotSink(path: String) : DecryptedTallyOrBallotSinkIF {
        val out: FileOutputStream = FileOutputStream(path, true) // append

        override fun writeDecryptedTallyOrBallot(tally: DecryptedTallyOrBallot) {
            val ballotProto: pbandk.Message = tally.publishProto()
            writeDelimitedTo(ballotProto, out)
        }

        override fun close() {
            out.close()
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    actual override fun pepBallotSink(outputDir: String): PepBallotSinkIF = PepBallotSink(outputDir)

    // TODO
    inner class PepBallotSink(path: String) : PepBallotSinkIF {
        override fun writePepBallot(pepBallot : BallotPep) {
        }
        override fun close() {
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


    fun writeDelimitedTo(proto: pbandk.Message, output: OutputStream) {
        val bb = ByteArrayOutputStream()
        proto.encodeToStream(bb)
        writeVlen(bb.size(), output)
        output.write(bb.toByteArray())
        output.flush()
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

/** Make sure output directories exists and are writeable.  */
fun validateOutputDir(path: Path, error: Formatter): Boolean {
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