package electionguard.publish

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import electionguard.ballot.DecryptionResult
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.Manifest
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.protoconvert.import
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
import mu.KotlinLogging
import pbandk.decodeFromByteArray
import platform.posix.FILE
import platform.posix.fclose
import platform.posix.fread
import platform.posix.lstat
import platform.posix.stat

internal val logger = KotlinLogging.logger("nativeReader")
internal val MAX_RECORD = 1000 * 1000

//// Used by native ConsumerProto

fun makeManifestInternal(manifestBytes: ByteArray): Result<Manifest, String> {
    val proto = electionguard.protogen.Manifest.decodeFromByteArray(manifestBytes)
    return proto.import()
}

fun readElectionConfig(filename: String): Result<ElectionConfig, String> {
    val buffer = gulp(filename)
    val proto = electionguard.protogen.ElectionConfig.decodeFromByteArray(buffer)
    return proto.import()
}

fun GroupContext.readElectionInitialized(filename: String): Result<ElectionInitialized, String> {
    val buffer = gulp(filename)
    val proto = electionguard.protogen.ElectionInitialized.decodeFromByteArray(buffer)
    return proto.import(this)
}

fun GroupContext.readTallyResult(filename: String): Result<TallyResult, String> {
    val buffer = gulp(filename)
    val proto = electionguard.protogen.TallyResult.decodeFromByteArray(buffer)
    return proto.import(this)
}

fun GroupContext.readDecryptionResult(filename: String): Result<DecryptionResult, String> {
    val buffer = gulp(filename)
    val proto = electionguard.protogen.DecryptionResult.decodeFromByteArray(buffer)
    return proto.import(this)
}

class PlaintextBallotIterator(
    private val filename: String,
    val filter : ((PlaintextBallot) -> Boolean)?,
) : AbstractIterator<PlaintextBallot>() {

    private val file = openFile(filename, "rb")

    override fun computeNext() {
        while (true) {
            val length = readVlen(file, filename)
            if (length <= 0) {
                fclose(file)
                return done()
            }
            val message = readFromFile(file, length.toULong(), filename)
            val ballotProto = electionguard.protogen.PlaintextBallot.decodeFromByteArray(message)
            val ballot = ballotProto.import()
            if (filter != null && !filter.invoke(ballot)) {
                continue
            }
            setNext(ballot)
            break
        }
    }
}

class EncryptedBallotIterator(
    private val groupContext: GroupContext,
    private val filename: String,
    private val protoFilter: ((electionguard.protogen.EncryptedBallot) -> Boolean)?,
    private val filter: ((EncryptedBallot) -> Boolean)?,
) : AbstractIterator<EncryptedBallot>() {
    private val file = openFile(filename, "rb")

    override fun computeNext() {
        while (true) {
            val length = readVlen(file, filename)
            if (length <= 0) {
                fclose(file)
                return done()
            }
            val message = readFromFile(file, length.toULong(), filename)
            val ballotProto = electionguard.protogen.EncryptedBallot.decodeFromByteArray(message)
            if (protoFilter?.invoke(ballotProto) == false) {
                continue // skip it
            }
            val ballotResult = ballotProto.import(groupContext)
            if (ballotResult is Ok) {
                if (filter?.invoke(ballotResult.unwrap()) == false) {
                    continue // skip it
                }
                setNext(ballotResult.unwrap())
                break
            } else {
                logger.warn { "Error on ${ballotProto.ballotId} = ${ballotResult.unwrapError()}" }
                continue
            }
        }
    }
}

class SpoiledBallotTallyIterator(
    private val group: GroupContext,
    private val filename: String,
) : AbstractIterator<DecryptedTallyOrBallot>() {

    private val file = openFile(filename, "rb")

    override fun computeNext() {
        val length = readVlen(file, filename)
        if (length <= 0) {
            fclose(file)
            return done()
        }
        val message = readFromFile(file, length.toULong(), filename)
        val tallyProto = electionguard.protogen.DecryptedTallyOrBallot.decodeFromByteArray(message)
        val tally = tallyProto.import(group)
        setNext(tally.getOrElse { throw RuntimeException("DecryptedTallyOrBallot failed to parse") })
    }
}

fun GroupContext.readTrustee(filename: String): DecryptingTrusteeIF {
    val buffer = gulp(filename)
    val trusteeProto = electionguard.protogen.DecryptingTrustee.decodeFromByteArray(buffer)
    return trusteeProto.import(this).getOrElse { throw RuntimeException("DecryptingTrustee $filename failed to parse") }
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


//// Used by native ConsumerProto and ConsumerJson

/** Read everything in the file and return as a ByteArray. */
@Throws(IOException::class)
fun gulp(filename: String): ByteArray {
    return memScoped {
        val stat = alloc<stat>()
        // lstat(@kotlinx.cinterop.internal.CCall.CString __file: kotlin.String?,
        //   __buf: kotlinx.cinterop.CValuesRef<platform.posix.stat>?)
        // : kotlin.Int { /* compiled code */ }
        if (lstat(filename, stat.ptr) != 0) {
            checkErrno {mess -> throw IOException("Fail lstat $mess on $filename")}
        }
        val size = stat.st_size.toULong()
        val file = openFile(filename, "rb")
        val ba = readFromFile(file, size, filename)
        fclose(file)

        return@memScoped ba
    }
}

@Throws(IOException::class)
fun readFromFile(file: CPointer<FILE>, nbytes : ULong, filename : String): ByteArray {
    return memScoped {
        val bytePtr: CArrayPointer<ByteVar> = allocArray(nbytes.toInt())

        // fread(
        //   __ptr: kotlinx.cinterop.CValuesRef<*>?,
        //   __size: platform.posix.size_t /* = kotlin.ULong */,
        //   __n: platform.posix.size_t /* = kotlin.ULong */,
        //   __stream: kotlinx.cinterop.CValuesRef<platform.posix.FILE /* = platform.posix._IO_FILE */>?)
        //   : kotlin.ULong { /* compiled code */ }
        val nread = fread(bytePtr, 1, nbytes, file)
        if (nread < 0U) {
            checkErrno { mess -> throw IOException("Fail read $mess on '$filename'") }
        }
        if (nread != nbytes) {
            throw IOException("Fail read $nread != $nbytes on '$filename'")
        }
        return@memScoped bytePtr.readBytes(nread.toInt())
    }
}
