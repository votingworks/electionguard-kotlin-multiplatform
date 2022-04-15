package electionguard.publish

import electionguard.ballot.*
import electionguard.protoconvert.publishElectionRecord
import electionguard.protoconvert.publishPlaintextBallot
import electionguard.protoconvert.publishPlaintextTally
import electionguard.protoconvert.publishSubmittedBallot
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
actual class Publisher {
    private val topDir: String
    private val createPublisherMode: PublisherMode
    private var path: ElectionRecordPath

    actual constructor(topDir: String, publisherMode: PublisherMode) {
        this.topDir = topDir
        this.createPublisherMode = publisherMode
        this.path = ElectionRecordPath(topDir)

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


    /** Publishes the entire election record as proto.  */
    actual fun writeElectionRecordProto(
        manifest: Manifest,
        constants: ElectionConstants,
        context: ElectionContext?,
        guardianRecords: List<GuardianRecord>?,
        devices: Iterable<EncryptionDevice>?,
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
            writeSubmittedBallots(submittedBallots)
        }
        if (spoiledBallots != null) {
            writeSpoiledBallots(spoiledBallots)
        }

        val electionRecordProto = publishElectionRecord(
            ElectionRecordPath.PROTO_VERSION,
            manifest,
            constants,
            context,
            guardianRecords,
            devices,
            ciphertextTally,
            decryptedTally,
            availableGuardians
        )
        writeElectionRecord(electionRecordProto)
    }

    @Throws(IOException::class)
    fun writeElectionRecord(proto: electionguard.protogen.ElectionRecord) {
        val fileout = path.electionRecordProtoPath()
        val file: CPointer<FILE> = openFile(fileout)
        val buffer = proto.encodeToByteArray()
        try {
            writeToFile(file, fileout, buffer)
        } finally {
            fflush(file)
            fclose(file)
        }
    }

    @Throws(IOException::class)
    fun writeSubmittedBallots(submittedBallots: Iterable<SubmittedBallot>): Boolean {
        val fileout = path.submittedBallotProtoPath()
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
        val fileout = path.spoiledBallotProtoPath()
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
    actual fun writeInvalidBallots(invalidDir: String, invalidBallots: List<PlaintextBallot>) {
        if (!invalidBallots.isEmpty()) {
            val fileout = path.invalidBallotProtoPath(invalidDir)
            val file: CPointer<FILE> = openFile(fileout)
            try {
                invalidBallots.forEach {
                    val proto = it.publishPlaintextBallot()
                    val buffer = proto.encodeToByteArray()
                    val length = writeVlen(file, fileout, buffer.size)
                    if (length <= 0) {
                        fclose(file)
                        throw IOException("write failed on $invalidDir")
                    }
                    writeToFile(file, fileout, buffer)
                }
            } finally {
                fclose(file)
            }
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
