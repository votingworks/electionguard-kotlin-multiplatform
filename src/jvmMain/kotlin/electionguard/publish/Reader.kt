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
import electionguard.ballot.PlaintextTally
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrustee
import electionguard.protoconvert.importDecryptingTrustee
import electionguard.protoconvert.importDecryptionResult
import electionguard.protoconvert.importElectionConfig
import electionguard.protoconvert.importElectionInitialized
import electionguard.protoconvert.importPlaintextBallot
import electionguard.protoconvert.importPlaintextTally
import electionguard.protoconvert.importEncryptedBallot
import electionguard.protoconvert.importTallyResult
import pbandk.decodeFromByteBuffer
import pbandk.decodeFromStream
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.function.Predicate

// The low level reading functions

fun readElectionConfig(filename: String): Result<ElectionConfig, String> {
    var proto: electionguard.protogen.ElectionConfig
    FileInputStream(filename).use { inp -> proto = electionguard.protogen.ElectionConfig.decodeFromStream(inp) }
    return importElectionConfig(proto)
}

fun GroupContext.readElectionInitialized(filename: String): Result<ElectionInitialized, String> {
    var proto: electionguard.protogen.ElectionInitialized
    FileInputStream(filename).use { inp -> proto = electionguard.protogen.ElectionInitialized.decodeFromStream(inp) }
    return this.importElectionInitialized(proto)
}

fun GroupContext.readTallyResult(filename: String): Result<TallyResult, String> {
    var proto: electionguard.protogen.TallyResult
    FileInputStream(filename).use { inp -> proto = electionguard.protogen.TallyResult.decodeFromStream(inp) }
    return this.importTallyResult(proto)
}

fun GroupContext.readDecryptionResult(filename: String): Result<DecryptionResult, String> {
    var proto: electionguard.protogen.DecryptionResult
    FileInputStream(filename).use { inp -> proto = electionguard.protogen.DecryptionResult.decodeFromStream(inp) }
    return this.importDecryptionResult(proto)
}

class PlaintextBallotIterator(
    private val group: GroupContext,
    filename: String,
    private val filter: Predicate<PlaintextBallot>?
) : AbstractIterator<PlaintextBallot>() {
    private val input: FileInputStream = FileInputStream(filename)

    override fun computeNext() {
        while (true) {
            val length = readVlen(input)
            if (length < 0) {
                input.close()
                return done()
            }
            val message = input.readNBytes(length)
            val ballotProto = electionguard.protogen.PlaintextBallot.decodeFromByteBuffer(ByteBuffer.wrap(message))
            val ballot = group.importPlaintextBallot(ballotProto)
            if (filter != null && !filter.test(ballot)) {
                continue // skip it
            }
            setNext(ballot)
            break
        }
    }
}

// Create iterators, so that we never have to read in all ballots at once.
class EncryptedBallotIterator(
    filename: String,
    private val groupContext: GroupContext,
    private val protoFilter: Predicate<electionguard.protogen.EncryptedBallot>?,
    private val filter: Predicate<EncryptedBallot>?,
) : AbstractIterator<EncryptedBallot>() {

    private val input: FileInputStream = FileInputStream(filename)

    override fun computeNext() {
        while (true) {
            val length = readVlen(input)
            if (length < 0) {
                input.close()
                return done()
            }
            val message = input.readNBytes(length)
            val ballotProto = electionguard.protogen.EncryptedBallot.decodeFromByteBuffer(ByteBuffer.wrap(message))
            if (protoFilter != null && !protoFilter.test(ballotProto)) {
                continue // skip it
            }
            val ballotResult = groupContext.importEncryptedBallot(ballotProto)
            if (ballotResult is Ok) {
                if (filter != null && !filter.test(ballotResult.unwrap())) {
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
    filename: String,
    private val groupContext: GroupContext,
) : AbstractIterator<PlaintextTally>() {
    private val input: FileInputStream = FileInputStream(filename)

    override fun computeNext() {
        val length = readVlen(input)
        if (length < 0) {
            input.close()
            return done()
        }
        val message = input.readNBytes(length)
        val tallyProto = electionguard.protogen.PlaintextTally.decodeFromByteBuffer(ByteBuffer.wrap(message))
        val tally = groupContext.importPlaintextTally(tallyProto)
        setNext(tally.getOrElse { throw RuntimeException("Tally failed to parse") })
    }
}

fun GroupContext.readTrustee(filename: String): DecryptingTrustee {
    var proto: electionguard.protogen.DecryptingTrustee
    FileInputStream(filename).use { inp -> proto = electionguard.protogen.DecryptingTrustee.decodeFromStream(inp) }
    return this.importDecryptingTrustee(proto).getOrElse { throw RuntimeException("DecryptingTrustee $filename failed to parse") }
}

// variable length (base 128) int32
private fun readVlen(input: InputStream): Int {
    var ib: Int = input.read()
    if (ib == -1) {
        return -1
    }

    var result = ib.and(0x7F)
    var shift = 7
    while (ib.and(0x80) != 0) {
        ib = input.read()
        if (ib == -1) {
            return -1
        }
        val im = ib.and(0x7F).shl(shift)
        result = result.or(im)
        shift += 7
    }
    return result
}