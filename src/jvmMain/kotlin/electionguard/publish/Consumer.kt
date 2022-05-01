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
import electionguard.ballot.SubmittedBallot
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrustee
import electionguard.protoconvert.importDecryptingTrustee
import electionguard.protoconvert.importDecryptionResult
import electionguard.protoconvert.importElectionConfig
import electionguard.protoconvert.importElectionInitialized
import electionguard.protoconvert.importPlaintextBallot
import electionguard.protoconvert.importPlaintextTally
import electionguard.protoconvert.importSubmittedBallot
import electionguard.protoconvert.importTallyResult
import pbandk.decodeFromByteBuffer
import pbandk.decodeFromStream
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.function.Predicate

fun GroupContext.readElectionConfig(filename: String): Result<ElectionConfig, String> {
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
    val group: GroupContext,
    filename: String,
    val filter: Predicate<PlaintextBallot>
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
            if (!filter.test(ballot)) {
                continue // skip it
            }
            setNext(ballot)
            break
        }
    }
}

// Create iterators, so that we never have to read in all ballots at once.
class SubmittedBallotIterator(
    filename: String,
    val groupContext: GroupContext,
    val filter: Predicate<electionguard.protogen.SubmittedBallot>,
) : AbstractIterator<SubmittedBallot>() {

    private val input: FileInputStream = FileInputStream(filename)

    override fun computeNext() {
        while (true) {
            val length = readVlen(input)
            if (length < 0) {
                input.close()
                return done()
            }
            val message = input.readNBytes(length)
            val ballotProto = electionguard.protogen.SubmittedBallot.decodeFromByteBuffer(ByteBuffer.wrap(message))
            if (!filter.test(ballotProto)) {
                continue // skip it
            }
            val ballotResult = groupContext.importSubmittedBallot(ballotProto)
            if (ballotResult is Ok) {
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
    val groupContext: GroupContext,
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
        setNext(tally.getOrElse { throw RuntimeException("Tally didnt parse") })
    }
}

fun GroupContext.readTrustee(filename: String): DecryptingTrustee {
    var proto: electionguard.protogen.DecryptingTrustee
    FileInputStream(filename).use { inp -> proto = electionguard.protogen.DecryptingTrustee.decodeFromStream(inp) }
    return this.importDecryptingTrustee(proto).getOrElse { throw RuntimeException("DecryptingTrustee $filename didnt parse") }
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