package electionguard.publish

import electionguard.ballot.ElectionRecord
import electionguard.ballot.ElectionRecordAllData
import electionguard.ballot.PlaintextTally
import electionguard.ballot.SubmittedBallot
import electionguard.core.GroupContext
import electionguard.protoconvert.importElectionRecord
import electionguard.protoconvert.importPlaintextTally
import electionguard.protoconvert.importSubmittedBallot
import io.ktor.utils.io.errors.*
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.value
import pbandk.decodeFromByteArray
import platform.posix.FILE
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.lstat
import platform.posix.posix_errno
import platform.posix.stat
import platform.posix.strerror

actual class Consumer actual constructor(topDir: String, val groupContext: GroupContext) {
    val path = ElectionRecordPath(topDir)

    @Throws(IOException::class)
    actual fun readElectionRecordAllData(): ElectionRecordAllData {
        val electionRecord = readElectionRecord()

        return ElectionRecordAllData(
            electionRecord.protoVersion,
            electionRecord.constants,
            electionRecord.manifest,
            electionRecord.context?: throw RuntimeException("missing context"),
            electionRecord.guardianRecords?: emptyList(),
            electionRecord.devices?: emptyList(),
            electionRecord.encryptedTally?: throw RuntimeException("missing encryptedTally"),
            electionRecord.decryptedTally?: throw RuntimeException("missing decryptedTally"),
            electionRecord.availableGuardians?: emptyList(),
            iterateSubmittedBallots(),
            iterateSpoiledBallotTallies(),
        )
    }

    @Throws(IOException::class)
    actual fun readElectionRecord(): ElectionRecord {
        val proto = electionguard.protogen.ElectionRecord.decodeFromByteArray(gulp(path.electionRecordProtoPath()))
        return proto.importElectionRecord(groupContext)
    }

    actual fun iterateSubmittedBallots(): Iterable<SubmittedBallot> {
        return Iterable { SubmittedBallotIterator(path.submittedBallotProtoPath()) { true } }
    }

    actual fun iterateCastBallots(): Iterable<SubmittedBallot> {
        return Iterable { SubmittedBallotIterator(path.submittedBallotProtoPath())
            { it.state === electionguard.protogen.SubmittedBallot.BallotState.CAST }
        }
    }

    actual fun iterateSpoiledBallots(): Iterable<SubmittedBallot> {
        return Iterable { SubmittedBallotIterator(path.submittedBallotProtoPath())
            { it.state === electionguard.protogen.SubmittedBallot.BallotState.SPOILED }
        }
    }

    private inner class SubmittedBallotIterator(
        val filename: String,
        val filter: (electionguard.protogen.SubmittedBallot) -> Boolean,
    ) : AbstractIterator<SubmittedBallot>() {

        private val file = openFile(filename)

        override fun computeNext() {
            while (true) {
                val length = readVlen(file, filename)
                if (length <= 0) {
                    fclose(file)
                    return done()
                }
                val message = readFromFile(file, length.toULong(), filename)
                val ballotProto = electionguard.protogen.SubmittedBallot.decodeFromByteArray(message)
                if (!filter(ballotProto)) {
                    continue // skip it
                }
                val ballot =
                    ballotProto.importSubmittedBallot(groupContext) ?: throw RuntimeException("Ballot didnt parse")
                setNext(ballot)
                break
            }
        }
    }

    // all spoiled ballot tallies
    actual fun iterateSpoiledBallotTallies(): Iterable<PlaintextTally> {
        return Iterable { SpoiledBallotTallyIterator(path.spoiledBallotProtoPath())}
    }

    private inner class SpoiledBallotTallyIterator(
        val filename: String,
    ) : AbstractIterator<PlaintextTally>() {

        private val file = openFile(filename)

        override fun computeNext() {
            val length = readVlen(file, filename)
            if (length <= 0) {
                fclose(file)
                return done()
            }
            val message = readFromFile(file, length.toULong(), filename)
            val tallyProto = electionguard.protogen.PlaintextTally.decodeFromByteArray(message)
            val tally = tallyProto.importPlaintextTally(groupContext) ?: throw RuntimeException("Tally didnt parse")
            setNext(tally)
        }
    }
}

@Throws(IOException::class)
private fun openFile(filename : String) : CPointer<FILE> {
    // fopen(
    //       @kotlinx.cinterop.internal.CCall.CString __filename: kotlin.String?,
    //       @kotlinx.cinterop.internal.CCall.CString __modes: kotlin.String?)
    //       : kotlinx.cinterop.CPointer<platform.posix.FILE>?
    val file = fopen(filename, "rb")
    val errno = posix_errno()
    if (errno != 0 || file == null) {
        throw IOException("Fail open " + strerror(errno).toString() + " on " + filename)
    }
    return file
}

@Throws(IOException::class)
private fun readFromFile(file: CPointer<FILE>, nbytes : ULong, filename : String): ByteArray {
    return memScoped {
        val bytePtr : CArrayPointer<ByteVar> = allocArray(nbytes.toInt())

        // fread(
        //   __ptr: kotlinx.cinterop.CValuesRef<*>?,
        //   __size: platform.posix.size_t /* = kotlin.ULong */,
        //   __n: platform.posix.size_t /* = kotlin.ULong */,
        //   __stream: kotlinx.cinterop.CValuesRef<platform.posix.FILE /* = platform.posix._IO_FILE */>?)
        //   : kotlin.ULong { /* compiled code */ }
        val nread = fread(bytePtr, 1, nbytes.toULong(), file)
        val errno = posix_errno()
        if (errno != 0) {
            throw IOException("Fail read " + strerror(errno).toString() + " on " + filename)
        }
        if (nread != nbytes) {
            throw IOException("Fail read $nread != $nbytes  on $filename")
        }

        val ba: ByteArray = bytePtr.readBytes(nread.toInt())
        return@memScoped ba
    }
}

/** Read everything in the file and return as a ByteArray. */
@Throws(IOException::class)
private fun gulp(filename: String): ByteArray {
    return memScoped {
        val stat = alloc<stat>()
        // fstat(__fd: kotlin.Int,
        //   __buf: kotlinx.cinterop.CValuesRef<platform.posix.stat>?)
        //   : kotlin.Int { /* compiled code */ }
        if (lstat(filename, stat.ptr) != 0) {
            val errno = posix_errno()
            throw IOException("Fail lstat " + strerror(errno).toString() + " on " + filename)
        }
        val size = stat.st_size.toULong()

        val file = openFile(filename)
        val ba = readFromFile(file, size, filename)
        fclose(file)

        return@memScoped ba
    }
}

/** read variable length (base 128) integer from a stream and return as an Int */
@Throws(IOException::class)
private fun readVlen(input: CPointer<FILE>, filename: String): Int {
    var ib: Int = readByte(input, filename)
    if (ib == 0) {
        return 0
    }

    var result = ib.and(0x7F)
    var shift = 7
    while (ib.and(0x80) != 0) {
        ib = readByte(input, filename)
        if (ib == -1) {
            return -1
        }
        val im = ib.and(0x7F).shl(shift)
        result = result.or(im)
        shift += 7
    }
    return result
}

/** read a single byte from a stream and return as an Int */
@Throws(IOException::class)
private fun readByte(file: CPointer<FILE>, filename: String): Int {
    return memScoped {
        val intPtr = alloc<IntVar>()
        fread(intPtr.ptr, 1, 1, file)
        val errno = posix_errno()
        if (errno != 0) {
            throw IOException("Fail readByte " + strerror(errno).toString() + " on " + filename)
        }
        return@memScoped intPtr.value
    }
}
