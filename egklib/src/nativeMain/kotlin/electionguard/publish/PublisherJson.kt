package electionguard.publish

import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.DecryptionResult
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.TallyResult
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.protoconvert.publishDecryptingTrusteeProto
import electionguard.protoconvert.publishProto
import io.ktor.utils.io.errors.*
import kotlinx.cinterop.CPointer
import pbandk.encodeToByteArray
import platform.posix.FILE
import platform.posix.fclose
import platform.posix.fflush

/** Write the Election Record as protobuf files.  */
actual class PublisherJson actual constructor(private val topDir: String, createNew: Boolean) : Publisher {
    private var path = ElectionRecordProtoPaths(topDir)

    init {
        if (createNew) {
            if (!exists(topDir)) {
                createDirectories(topDir)
            }
        } else {
            if (!exists(topDir)) {
                createDirectories(topDir)
            }
        }
    }

    actual override fun writeElectionConfig(config: ElectionConfig) {
        val proto = config.publishProto()
        val buffer = proto.encodeToByteArray()

        val fileout = path.electionConfigPath()
        val file: CPointer<FILE> = openFile(fileout, "wb")
        try {
            writeToFile(file, fileout, buffer)
        } finally {
            fflush(file)
            fclose(file)
        }
    }

    actual override fun writeElectionInitialized(init: ElectionInitialized) {
        val proto = init.publishProto()
        val buffer = proto.encodeToByteArray()

        val fileout = path.electionInitializedPath()
        val file: CPointer<FILE> = openFile(fileout, "wb")
        try {
            writeToFile(file, fileout, buffer)
        } finally {
            fflush(file)
            fclose(file)
        }
    }

    actual override fun writeEncryptions(
        init: ElectionInitialized,
        ballots: Iterable<EncryptedBallot>
    ) {
        writeElectionInitialized(init)
        val sink = encryptedBallotSink()
        ballots.forEach {sink.writeEncryptedBallot(it) }
        sink.close()
    }

    actual override fun writeTallyResult(tally: TallyResult) {
        val proto = tally.publishProto()
        val buffer = proto.encodeToByteArray()

        val fileout = path.tallyResultPath()
        val file: CPointer<FILE> = openFile(fileout, "wb")
        try {
            writeToFile(file, fileout, buffer)
        } finally {
            fflush(file)
            fclose(file)
        }
    }

    actual override fun writeDecryptionResult(decryption: DecryptionResult) {
        val proto = decryption.publishProto()
        val buffer = proto.encodeToByteArray()

        val fileout = path.decryptionResultPath()
        val file: CPointer<FILE> = openFile(fileout, "wb")
        try {
            writeToFile(file, fileout, buffer)
        } finally {
            fflush(file)
            fclose(file)
        }
    }

    @Throws(IOException::class)
    fun writeSpoiledBallotTallies(spoiledBallots: Iterable<DecryptedTallyOrBallot>): Boolean {
        val fileout = path.spoiledBallotPath()
        val file: CPointer<FILE> = openFile(fileout, "wb")
        try {
            spoiledBallots.forEach {
                val proto = it.publishProto()
                val buffer = proto.encodeToByteArray()

                val length = writeVlen(file, fileout, buffer.size)
                if (length <= 0) {
                    fclose(file)
                    return false
                }
                writeToFile(file, fileout, buffer)
            }
        } finally {
            fclose(file)
        }
        return true
    }

    actual override fun writePlaintextBallot(outputDir: String, plaintextBallots: List<PlaintextBallot>) {
        if (plaintextBallots.isNotEmpty()) {
            val fileout = path.plaintextBallotPath(outputDir)
            val file: CPointer<FILE> = openFile(fileout, "wb")
            try {
                plaintextBallots.forEach {
                    val proto = it.publishProto()
                    val buffer = proto.encodeToByteArray()
                    val length = writeVlen(file, fileout, buffer.size)
                    if (length <= 0) {
                        fclose(file)
                        throw IOException("write failed on $outputDir")
                    }
                    writeToFile(file, fileout, buffer)
                }
            } finally {
                fclose(file)
            }
        }
    }

    actual override fun writeTrustee(trusteeDir: String, trustee: KeyCeremonyTrustee) {
        val proto = trustee.publishDecryptingTrusteeProto()
        val buffer = proto.encodeToByteArray()

        val fileout = path.decryptingTrusteePath(trusteeDir, trustee.id)
        val file: CPointer<FILE> = openFile(fileout, "wb")
        try {
            writeToFile(file, fileout, buffer)
        } finally {
            fflush(file)
            fclose(file)
        }
    }

    actual override fun encryptedBallotSink(): EncryptedBallotSinkIF =
        EncryptedBallotSink(path.encryptedBallotPath())

    private inner class EncryptedBallotSink(val fileout: String) : EncryptedBallotSinkIF {
        val file: CPointer<FILE> = openFile(fileout, "wb")

        override fun writeEncryptedBallot(ballot: EncryptedBallot) {
            val ballotProto: pbandk.Message = ballot.publishProto()
            val buffer = ballotProto.encodeToByteArray()

            val length = writeVlen(file, fileout, buffer.size)
            if (length <= 0) {
                fclose(file)
                throw IOException("write failed on $fileout")
            }
            writeToFile(file, fileout, buffer)
        }

        override fun close() {
            fclose(file)
        }
    }

    actual override fun decryptedTallyOrBallotSink(): DecryptedTallyOrBallotSinkIF =
        DecryptedTallyOrBallotSink(path.spoiledBallotPath())

    private inner class DecryptedTallyOrBallotSink(val fileout: String) : DecryptedTallyOrBallotSinkIF {
        val file: CPointer<FILE> = openFile(fileout, "wb")

        override fun writeDecryptedTallyOrBallot(tally: DecryptedTallyOrBallot) {
            val ballotProto: pbandk.Message = tally.publishProto()
            val buffer = ballotProto.encodeToByteArray()

            val length = writeVlen(file, fileout, buffer.size)
            if (length <= 0) {
                fclose(file)
                throw IOException("write failed on $fileout")
            }
            writeToFile(file, fileout, buffer)
        }

        override fun close() {
            fclose(file)
        }
    }
}