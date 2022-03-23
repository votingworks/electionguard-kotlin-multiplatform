package electionguard.publish

import electionguard.ballot.*
import electionguard.protoconvert.publishElectionRecord
import electionguard.protoconvert.publishPlaintextTally
import electionguard.protoconvert.publishSubmittedBallot
import io.ktor.utils.io.errors.*
import pbandk.encodeToStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** Publishes the Manifest Record to Json or protobuf files.  */
actual class Publisher {
    private val topdir: String
    private val createPublisherMode: PublisherMode
    private val electionRecordDir: Path

    actual constructor(where: String, publisherMode: PublisherMode) {
        this.topdir = where
        this.createPublisherMode = publisherMode
        this.electionRecordDir = Path.of(where).resolve(ELECTION_RECORD_DIR)
        
        if (createPublisherMode == PublisherMode.createNew) {
            if (!Files.exists(electionRecordDir)) {
                Files.createDirectories(electionRecordDir)
            } else {
                removeAllFiles()
            }
        } else if (createPublisherMode == PublisherMode.createIfMissing) {
            if (!Files.exists(electionRecordDir)) {
                Files.createDirectories(electionRecordDir)
            }
        } else {
            check(Files.exists(electionRecordDir)) { "Non existing election directory $electionRecordDir" }
        }
    }

    internal constructor(electionRecordDir: Path, createPublisherMode : PublisherMode) {
        this.createPublisherMode = createPublisherMode
        topdir = electionRecordDir.toAbsolutePath().toString()
        this.electionRecordDir = electionRecordDir
        
        if (createPublisherMode == PublisherMode.createNew) {
            if (!Files.exists(electionRecordDir)) {
                Files.createDirectories(electionRecordDir)
            } else {
                removeAllFiles()
            }

        } else if (createPublisherMode == PublisherMode.createIfMissing) {
            if (!Files.exists(electionRecordDir)) {
                Files.createDirectories(electionRecordDir)
            }

        } else {
            check(Files.exists(electionRecordDir)) { "Non existing election directory $electionRecordDir" }
        }
    }

    /** Delete everything in the output directory, but leave that directory.  */
    @Throws(IOException::class)
    private fun removeAllFiles() {
        if (!electionRecordDir.toFile().exists()) {
            return
        }
        val filename: String = electionRecordDir.getFileName().toString()
        if (!filename.startsWith("election_record")) {
            throw RuntimeException(
                String.format(
                    "Publish directory '%s' should start with 'election_record'",
                    filename
                )
            )
        }
        Files.walk(electionRecordDir)
            .filter { p: Path -> p != electionRecordDir }
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
    fun electionRecordProtoPath(): Path {
        return electionRecordDir.resolve(ELECTION_RECORD_FILE_NAME).toAbsolutePath()
    }

    fun submittedBallotProtoPath(): Path {
        return electionRecordDir.resolve(SUBMITTED_BALLOT_PROTO).toAbsolutePath()
    }

    fun spoiledBallotProtoPath(): Path {
        return electionRecordDir.resolve(SPOILED_BALLOT_FILE).toAbsolutePath()
    }

    /** Publishes the entire election record as proto.  */
    @Throws(IOException::class)
    actual fun writeElectionRecordProto(
        manifest: Manifest,
        context: ElectionContext,
        constants: ElectionConstants,
        guardianRecords: List<GuardianRecord>,
        devices: Iterable<EncryptionDevice>,
        submittedBallots: Iterable<SubmittedBallot>?,
        ciphertextTally: CiphertextTally?,
        decryptedTally: PlaintextTally?,
        spoiledBallots: Iterable<PlaintextTally>?,
        availableGuardians: List<AvailableGuardian>?
    ) {
        if (createPublisherMode == PublisherMode.readonly) {
            throw UnsupportedOperationException("Trying to write to readonly election record")
        }
        if (submittedBallots != null) {
            FileOutputStream(submittedBallotProtoPath().toFile()).use { out ->
                for (ballot in submittedBallots) {
                    val ballotProto = ballot.publishSubmittedBallot()
                    ballotProto.encodeToStream(out)
                }
            }
        }
        if (spoiledBallots != null) {
            FileOutputStream(spoiledBallotProtoPath().toFile()).use { out ->
                for (ballot in spoiledBallots) {
                    val ballotProto = ballot.publishPlaintextTally()
                    ballotProto.encodeToStream(out)
                }
            }
        }

        /*
        translateToProto(
        version : String,
        manifest: Manifest,
        context: ElectionContext,
        constants: ElectionConstants,
        guardianRecords: List<GuardianRecord>?,
        devices: Iterable<EncryptionDevice>,
        encryptedTally: CiphertextTally?,
        decryptedTally: PlaintextTally?,
        availableGuardians: List<AvailableGuardian>?,
         */
        val electionRecordProto = publishElectionRecord(
            PROTO_VERSION,
            manifest,
            constants,
            context,
            guardianRecords,
            devices,
            ciphertextTally,
            decryptedTally,
            availableGuardians
        )
        FileOutputStream(electionRecordProtoPath().toFile()).use {
                out -> electionRecordProto.encodeToStream(out) }
    }

    /** Copy accepted ballots file from the inputDir to this election record.  */
    @Throws(IOException::class)
    fun copyAcceptedBallots(inputDir: String) {
        if (createPublisherMode == PublisherMode.readonly) {
            throw UnsupportedOperationException("Trying to write to readonly election record")
        }
        val source: Path = Publisher(inputDir, PublisherMode.writeonly).submittedBallotProtoPath()
        val dest: Path = submittedBallotProtoPath()
        if (source == dest) {
            return
        }
        System.out.printf("Copy AcceptedBallots from %s to %s%n", source, dest)
        Files.copy(source, dest, StandardCopyOption.COPY_ATTRIBUTES)
    }

    companion object {
        const val PROTO_VERSION = "2.0.0"
        const val ELECTION_RECORD_DIR = "election_record"

        //// proto
        const val PROTO_SUFFIX = ".protobuf"
        const val ELECTION_RECORD_FILE_NAME = "electionRecord" + PROTO_SUFFIX
        const val GUARDIANS_FILE = "guardians" + PROTO_SUFFIX
        const val SUBMITTED_BALLOT_PROTO = "submittedBallots" + PROTO_SUFFIX
        const val SPOILED_BALLOT_FILE = "spoiledBallotsTally" + PROTO_SUFFIX
        const val TRUSTEES_FILE = "trustees" + PROTO_SUFFIX
    }
}