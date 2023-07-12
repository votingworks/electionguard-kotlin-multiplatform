package electionguard.publish

import electionguard.ballot.*
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.protoconvert.publishDecryptingTrusteeProto
import electionguard.protoconvert.publishProto
import io.ktor.utils.io.core.use
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
import platform.posix.fwrite

/** Write the Election Record as protobuf files.  */
actual class PublisherProto actual constructor(private val topDir: String, createNew: Boolean) : Publisher {
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

    actual override fun writeManifest(manifest: Manifest) : String {
        val proto = manifest.publishProto()
        val buffer = proto.encodeToByteArray()

        val fileout = path.manifestPath()
        val file: CPointer<FILE> = openFile(fileout, "wb")
        try {
            writeToFile(file, fileout, buffer)
        } finally {
            fclose(file)
        }
        return fileout
    }

    actual override fun writeElectionConfig(config: ElectionConfig) {
        val proto = config.publishProto()
        val buffer = proto.encodeToByteArray()

        val fileout = path.electionConfigPath()
        val file: CPointer<FILE> = openFile(fileout, "wb")
        try {
            writeToFile(file, fileout, buffer)
        } finally {
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
            fclose(file)
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
        val buffer = proto.encodeToByteArray()

        val fileout = path.tallyResultPath()
        val file: CPointer<FILE> = openFile(fileout, "wb")
        try {
            writeToFile(file, fileout, buffer)
        } finally {
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
            fclose(file)
        }
    }

    actual override fun encryptedBallotSink(): EncryptedBallotSinkIF =
        EncryptedBallotSink(path.encryptedBallotPath())

    actual override fun encryptedBallotSink(device: String): EncryptedBallotSinkIF =
        EncryptedBallotSink(path.encryptedBallotPath())

    actual override fun writeEncryptedBallotChain(closing: EncryptedBallotChain) {}

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

@Throws(IOException::class)
fun writeToFile(file: CPointer<FILE>, filename: String, buffer: ByteArray) {
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
        if (nwrite < buffer.size.toULong()) {
            checkErrno { mess -> throw IOException("Fail fwrite $mess on $filename") }
        }
        if (nwrite != buffer.size.toULong()) {
            throw IOException("Fail fwrite $nwrite != ${buffer.size}  on $filename")
        }
        // TODO add fflush()
    }
}

@Throws(IOException::class)
fun writeVlen(file: CPointer<FILE>, filename: String, length: Int): Int {
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
