package electionguard.publish

import electionguard.ballot.*
import electionguard.decrypt.DecryptingTrustee
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.protoconvert.publishDecryptingTrustee
import electionguard.protoconvert.publishDecryptionResult
import electionguard.protoconvert.publishElectionConfig
import electionguard.protoconvert.publishElectionInitialized
import electionguard.protoconvert.publishPlaintextBallot
import electionguard.protoconvert.publishPlaintextTally
import electionguard.protoconvert.publishSubmittedBallot
import electionguard.protoconvert.publishTallyResult
import io.ktor.utils.io.errors.*
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.set
import pbandk.encodeToByteArray
import platform.posix.FILE
import platform.posix.fclose
import platform.posix.fflush
import platform.posix.fopen
import platform.posix.fwrite

/** Write the Election Record as protobuf files.  */
actual class Publisher actual constructor(private val topDir: String, publisherMode: PublisherMode) {
    private val createPublisherMode: PublisherMode = publisherMode
    private var path = ElectionRecordPath(topDir)

    init {
        if (createPublisherMode == PublisherMode.createNew) {
            if (!exists(topDir)) {
                createDirectories(topDir)
            }
        } else if (createPublisherMode == PublisherMode.createIfMissing) {
            if (!exists(topDir)) {
                createDirectories(topDir)
            }
        } else {
            check(exists(topDir)) { "Non existing election directory $topDir" }
        }
    }

    actual fun writeElectionConfig(config: ElectionConfig) {
        val proto = config.publishElectionConfig()
        val buffer = proto.encodeToByteArray()

        val fileout = path.electionConfigPath()
        val file: CPointer<FILE> = openFile(fileout)
        try {
            writeToFile(file, fileout, buffer)
        } finally {
            fflush(file)
            fclose(file)
        }
    }

    actual fun writeElectionInitialized(init: ElectionInitialized) {
        val proto = init.publishElectionInitialized()
        val buffer = proto.encodeToByteArray()

        val fileout = path.electionInitializedPath()
        val file: CPointer<FILE> = openFile(fileout)
        try {
            writeToFile(file, fileout, buffer)
        } finally {
            fflush(file)
            fclose(file)
        }
    }

    actual fun writeEncryptions(
        init: ElectionInitialized,
        ballots: Iterable<SubmittedBallot>
    ) {
        writeElectionInitialized(init)
        val sink = submittedBallotSink()
        ballots.forEach {sink.writeSubmittedBallot(it) }
        sink.close()    }

    actual fun writeTallyResult(tally: TallyResult) {
        val proto = tally.publishTallyResult()
        val buffer = proto.encodeToByteArray()

        val fileout = path.tallyResultPath()
        val file: CPointer<FILE> = openFile(fileout)
        try {
            writeToFile(file, fileout, buffer)
        } finally {
            fflush(file)
            fclose(file)
        }
    }

    actual fun writeDecryptionResult(decryption: DecryptionResult) {
        val proto = decryption.publishDecryptionResult()
        val buffer = proto.encodeToByteArray()

        val fileout = path.decryptionResultPath()
        val file: CPointer<FILE> = openFile(fileout)
        try {
            writeToFile(file, fileout, buffer)
        } finally {
            fflush(file)
            fclose(file)
        }
    }

    @Throws(IOException::class)
    fun writeSubmittedBallots(submittedBallots: Iterable<SubmittedBallot>): Boolean {
        val fileout = path.submittedBallotPath()
        val file: CPointer<FILE> = openFile(fileout)
        try {
            submittedBallots.forEach {
                val proto = it.publishSubmittedBallot()
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

    @Throws(IOException::class)
    fun writeSpoiledBallots(spoiledBallots: Iterable<PlaintextTally>): Boolean {
        val fileout = path.spoiledBallotPath()
        val file: CPointer<FILE> = openFile(fileout)
        try {
            spoiledBallots.forEach {
                val proto = it.publishPlaintextTally()
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

    @Throws(IOException::class)
    actual fun writePlaintextBallot(outputDir: String, plaintextBallots: List<PlaintextBallot>) {
        if (plaintextBallots.isNotEmpty()) {
            val fileout = path.plaintextBallotPath(outputDir)
            val file: CPointer<FILE> = openFile(fileout)
            try {
                plaintextBallots.forEach {
                    val proto = it.publishPlaintextBallot()
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

    actual fun writeTrustee(trusteeDir: String, trustee: KeyCeremonyTrustee) {
        val proto = trustee.publishDecryptingTrustee()
        val buffer = proto.encodeToByteArray()

        val fileout = path.decryptingTrusteePath(trusteeDir, trustee.id)
        val file: CPointer<FILE> = openFile(fileout)
        try {
            writeToFile(file, fileout, buffer)
        } finally {
            fflush(file)
            fclose(file)
        }
    }

    actual fun submittedBallotSink(): SubmittedBallotSinkIF =
        SubmittedBallotSink(path.submittedBallotPath())

    inner class SubmittedBallotSink(val fileout: String) : SubmittedBallotSinkIF {
        val file: CPointer<FILE> = openFile(fileout)

        override fun writeSubmittedBallot(ballot: SubmittedBallot) {
            val ballotProto: pbandk.Message = ballot.publishSubmittedBallot()
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

@Throws(IOException::class)
private fun openFile(abspath: String): CPointer<FILE> {
    memScoped {
        // fopen(
        //       @kotlinx.cinterop.internal.CCall.CString __filename: kotlin.String?,
        //       @kotlinx.cinterop.internal.CCall.CString __modes: kotlin.String?)
        //       : kotlinx.cinterop.CPointer<platform.posix.FILE>?
        val file = fopen(abspath, "w+")
        if (file == null) {
            checkErrno { mess -> throw IOException("Fail open $mess on $abspath") }
        }
        return file!!
    }
}

@Throws(IOException::class)
private fun writeToFile(file: CPointer<FILE>, filename: String, buffer: ByteArray) {
    memScoped {
        val bytePtr: CArrayPointer<ByteVar> = allocArray(buffer.size)
        // TODO avoid copy
        buffer.forEachIndexed { index, element -> bytePtr[index] = element }

        // fwrite(
        //    __ptr: kotlinx.cinterop.CValuesRef<*>?,
        //    __size: platform.posix.size_t /* = kotlin.ULong */,
        //    __n: platform.posix.size_t /* = kotlin.ULong */,
        //    __s: kotlinx.cinterop.CValuesRef<platform.posix.FILE /* = platform.posix._IO_FILE */>?)
        // : kotlin.ULong { /* compiled code */ }
        val nwrite = fwrite(bytePtr, 1, buffer.size.toULong(), file)
        if (nwrite < 0u) {
            checkErrno { mess -> throw IOException("Fail fwrite $mess on $filename") }
        }
        if (nwrite != buffer.size.toULong()) {
            throw IOException("Fail fwrite $nwrite != $buffer.size  on $filename")
        }
    }
}

@Throws(IOException::class)
private fun writeVlen(file: CPointer<FILE>, filename: String, length: Int): Int {
    var value = length
    var count = 0

    // stolen from protobuf.CodedOutputStream.writeRawVarint32()
    while (true) {
        value = if (value and 0x7F.inv() == 0) {
            writeByte(file, filename, value.toByte())
            count++
            break
        } else {
            writeByte(file, filename, (value and 0x7F or 0x80).toByte())
            count++
            value ushr 7
        }
    }
    return count + 1
}

////////////////////////////////////////////////////////////

@Throws(IOException::class)
private fun writeByte(file: CPointer<FILE>, filename: String, b: Byte) {
    memScoped {
        val bytePtr: CArrayPointer<ByteVar> = allocArray(1)
        // TODO avoid copy
        bytePtr[0] = b

        val nwrite = fwrite(bytePtr, 1, 1, file)
        if (nwrite < 0u) {
            checkErrno { mess -> throw IOException("Fail writeByte $mess on $filename") }
        }
        if (nwrite.compareTo(1u) != 0) {
            throw IOException("Fail writeByte2 $nwrite on $filename")
        }
    }
}
