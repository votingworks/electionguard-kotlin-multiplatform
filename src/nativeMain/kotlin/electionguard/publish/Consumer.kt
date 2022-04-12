package electionguard.publish

import electionguard.ballot.ElectionRecord
import electionguard.ballot.ElectionRecordAllData
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.PlaintextTally
import electionguard.ballot.SubmittedBallot
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.protoconvert.importDecryptingTrustee
import electionguard.protoconvert.importElectionRecord
import electionguard.protoconvert.importPlaintextBallot
import electionguard.protoconvert.importPlaintextTally
import electionguard.protoconvert.importSubmittedBallot
import io.ktor.utils.io.errors.*
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import pbandk.decodeFromByteArray
import platform.posix.FILE
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.lstat
import platform.posix.opendir
import platform.posix.readdir
import platform.posix.stat

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
        val buffer = gulp(absPath(path.electionRecordProtoPath()))
        val proto = electionguard.protogen.ElectionRecord.decodeFromByteArray(buffer)
        return proto.importElectionRecord(groupContext)
    }

    actual fun iteratePlaintextBallots(ballotDir : String): Iterable<PlaintextBallot> {
        return Iterable { PlaintextBallotIterator(path.plaintextBallotProtoPath(ballotDir)) }
    }

    private inner class PlaintextBallotIterator(
        val filename: String,
    ) : AbstractIterator<PlaintextBallot>() {

        private val file = openFile(absPath(filename))

        override fun computeNext() {
            val length = readVlen(file, filename)
            if (length <= 0) {
                fclose(file)
                return done()
            }
            val message = readFromFile(file, length.toULong(), filename)
            val ballotProto = electionguard.protogen.PlaintextBallot.decodeFromByteArray(message)
            val ballot = ballotProto.importPlaintextBallot()
            setNext(ballot)
        }
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

        private val file = openFile(absPath(filename))

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

        private val file = openFile(absPath(filename))

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

    actual fun readTrustees(trusteeDir: String): List<DecryptingTrusteeIF> {
        val trustees = openDir(trusteeDir)

        val result = ArrayList<DecryptingTrusteeIF>()
        trustees.forEach {
            val filename = "$trusteeDir/$it"
            val buffer = gulpVlen(filename)
            val trusteeProto = electionguard.protogen.DecryptingTrustee.decodeFromByteArray(buffer)
            if (trusteeProto != null) {
                result.add(trusteeProto.importDecryptingTrustee(groupContext))
            }
        }
        return result
    }
}

@Throws(IOException::class)
private fun openDir(dirpath: String): List<String> {
    memScoped {
        // opendir(
        //    @kotlinx.cinterop.internal.CCall.CString __name: kotlin.String?)
        // : kotlinx.cinterop.CPointer<platform.posix.DIR /* = cnames.structs.__dirstream */>? { /* compiled code */ }
        val dir: CPointer<platform.posix.DIR>? = opendir(dirpath)
        if (dir == null) {
            checkErrno {mess -> throw IOException("Fail opendir $mess on $dirpath")}
        }
        println(" opendir $dir from $dirpath")

        // readdir(
        //    __dirp: kotlinx.cinterop.CValuesRef<platform.posix.DIR /* = cnames.structs.__dirstream */>?)
        // : kotlinx.cinterop.CPointer<platform.posix.dirent>? { /* compiled code */ }
        val result = ArrayList<String>()
        while (true) {
            val ddir: CPointer<platform.posix.dirent>? = readdir(dir)
            if (ddir == null) {
                // checkErrno { mess -> throw IOException("Fail readdir $mess on $dirpath") }
                break
            }
            val dirent: platform.posix.dirent = ddir!!.get(0)
            val filenamep: CArrayPointer<ByteVar> = dirent.d_name
            val filename = filenamep.toKString()
            if (filename.startsWith("remoteTrustee")) {
                result.add(filenamep.toKString())
            }
        }
        return result
    }
}

private val max : Int = 100
private fun CArrayPointer<ByteVar>.readZ(): String {
   val result = ByteArray(max)
   var idx = 0
   while (idx < max) {
       val b: Byte = this[idx]
       if (b.compareTo(0) == 0) {
           break;
       }
       result[idx] = b
       idx++
   }
    return result.toString()
}

@Throws(IOException::class)
private fun openFile(abspath: String): CPointer<FILE> {
    memScoped {
        // fopen(
        //       @kotlinx.cinterop.internal.CCall.CString __filename: kotlin.String?,
        //       @kotlinx.cinterop.internal.CCall.CString __modes: kotlin.String?)
        //       : kotlinx.cinterop.CPointer<platform.posix.FILE>?
        val file = fopen(abspath, "rb")
        if (file == null) {
            checkErrno {mess -> throw IOException("Fail open $mess on $abspath")}
        }
        return file!!
    }
}

/** Read everything in the file and return as a ByteArray. */
@Throws(IOException::class)
private fun gulp(filename: String): ByteArray {
    return memScoped {
        val stat = alloc<stat>()
        // lstat(@kotlinx.cinterop.internal.CCall.CString __file: kotlin.String?,
        //   __buf: kotlinx.cinterop.CValuesRef<platform.posix.stat>?)
        // : kotlin.Int { /* compiled code */ }
        if (lstat(filename, stat.ptr) != 0) {
            checkErrno {mess -> throw IOException("Fail lstat $mess on $filename")}
        }
        val size = stat.st_size.toULong()
        val file = openFile(absPath(filename))
        val ba = readFromFile(file, size, filename)
        fclose(file)

        return@memScoped ba
    }
}

/** Read first vlen record in the file and return as a ByteArray. */
@Throws(IOException::class)
private fun gulpVlen(filename: String): ByteArray {
    return memScoped {
        val file = openFile(filename)
        val length = readVlen(file, filename)
        val ba = readFromFile(file, length.toULong(), filename)
        fclose(file)

        return@memScoped ba
    }
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
        val nread = fread(bytePtr, 1, nbytes, file)
        if (nread < 0u) {
            checkErrno {mess -> throw IOException("Fail read $mess on $filename")}
        }
        if (nread != nbytes) {
            throw IOException("Fail read $nread != $nbytes  on $filename")
        }
        val ba: ByteArray = bytePtr.readBytes(nread.toInt())
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
        val nread = fread(intPtr.ptr, 1, 1, file)
        if (nread < 0u) {
            checkErrno { mess -> throw IOException("Fail readByte $mess on $filename") }
        }
        return@memScoped intPtr.value
    }
}
