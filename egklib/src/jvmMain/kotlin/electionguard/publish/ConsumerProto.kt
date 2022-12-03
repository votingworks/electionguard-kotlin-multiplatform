package electionguard.publish

import com.github.michaelbull.result.Err
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
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrustee
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.protoconvert.import
import mu.KotlinLogging
import pbandk.decodeFromByteBuffer
import pbandk.decodeFromStream
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Predicate

private val logger = KotlinLogging.logger("ElectionRecord")

actual class ConsumerProto actual constructor(val topDir: String, val groupContext: GroupContext): Consumer {
    val path = ElectionRecordProtoPaths(topDir)

    init {
        if (!Files.exists(Path.of(topDir))) {
            throw RuntimeException("Not existent directory $topDir")
        }
    }

    actual override fun topdir(): String {
        return this.topDir
    }

    actual override fun isJson() = false

    actual override fun readElectionConfig(): Result<ElectionConfig, String> {
        return readElectionConfig(path.electionConfigPath())
    }

    actual override fun readElectionInitialized(): Result<ElectionInitialized, String> {
        return groupContext.readElectionInitialized(path.electionInitializedPath())
    }

    actual override fun readTallyResult(): Result<TallyResult, String> {
        return groupContext.readTallyResult(path.tallyResultPath())
    }

    actual override fun readDecryptionResult(): Result<DecryptionResult, String> {
        return groupContext.readDecryptionResult(path.decryptionResultPath())
    }

    // all submitted ballots, cast or spoiled
    actual override fun iterateEncryptedBallots(
        filter: ((EncryptedBallot) -> Boolean)?
    ): Iterable<EncryptedBallot> {
        val filename = path.encryptedBallotPath()
        if (!Files.exists(Path.of(filename))) {
            return emptyList()
        }
        return Iterable { EncryptedBallotIterator(filename, groupContext, null, filter) }
    }

    actual override fun hasEncryptedBallots(): Boolean {
        return Files.exists(Path.of(path.encryptedBallotPath()))
    }

    // only EncryptedBallot that are CAST
    actual override fun iterateCastBallots(): Iterable<EncryptedBallot> {
        val filename = path.encryptedBallotPath()
        if (!Files.exists(Path.of(filename))) {
            return emptyList()
        }
        val protoFilter =
            Predicate<electionguard.protogen.EncryptedBallot> { it.state == electionguard.protogen.EncryptedBallot.BallotState.CAST }
        return Iterable { EncryptedBallotIterator(filename, groupContext, protoFilter, null) }
    }

    // only EncryptedBallot that are SPOILED
    actual override fun iterateSpoiledBallots(): Iterable<EncryptedBallot> {
        val filename = path.encryptedBallotPath()
        if (!Files.exists(Path.of(filename))) {
            return emptyList()
        }
        val protoFilter =
            Predicate<electionguard.protogen.EncryptedBallot> { it.state == electionguard.protogen.EncryptedBallot.BallotState.SPOILED }
        return Iterable { EncryptedBallotIterator(filename, groupContext, protoFilter, null) }
    }

    // all tallies in the SPOILED_BALLOT_FILE file
    actual override fun iterateDecryptedBallots(): Iterable<DecryptedTallyOrBallot> {
        val filename = path.spoiledBallotPath()
        if (!Files.exists(Path.of(filename))) {
            return emptyList()
        }
        return Iterable { SpoiledBallotTallyIterator(filename, groupContext) }
    }

    // plaintext ballots in given directory, with filter
    actual override fun iteratePlaintextBallots(
        ballotDir: String,
        filter: ((PlaintextBallot) -> Boolean)?
    ): Iterable<PlaintextBallot> {
        if (!Files.exists(Path.of(path.plaintextBallotPath(ballotDir)))) {
            return emptyList()
        }
        return Iterable { PlaintextBallotIterator(path.plaintextBallotPath(ballotDir), filter) }
    }

    // trustee in given directory for given guardianId
    actual override fun readTrustee(trusteeDir: String, guardianId: String): DecryptingTrusteeIF {
        val filename = path.decryptingTrusteePath(trusteeDir, guardianId)
        return groupContext.readTrustee(filename)
    }

    //////// The low level reading functions for protobuf

    private fun readElectionConfig(filename: String): Result<ElectionConfig, String> {
        return try {
            var proto: electionguard.protogen.ElectionConfig
            FileInputStream(filename).use { inp -> proto = electionguard.protogen.ElectionConfig.decodeFromStream(inp) }
            proto.import()
        } catch (e: Exception) {
            Err(e.message ?: "readElectionConfig $filename failed")
        }
    }

    private fun GroupContext.readElectionInitialized(filename: String): Result<ElectionInitialized, String> {
        return try {
            var proto: electionguard.protogen.ElectionInitialized
            FileInputStream(filename).use { inp ->
                proto = electionguard.protogen.ElectionInitialized.decodeFromStream(inp)
            }
            proto.import(this)
        } catch (e: Exception) {
            Err(e.message ?: "readElectionInitialized $filename failed")
        }
    }

    private fun GroupContext.readTallyResult(filename: String): Result<TallyResult, String> {
        return try {
            var proto: electionguard.protogen.TallyResult
            FileInputStream(filename).use { inp -> proto = electionguard.protogen.TallyResult.decodeFromStream(inp) }
            proto.import(this)
        } catch (e: Exception) {
            Err(e.message ?: "readTallyResult $filename failed")
        }
    }

    private fun GroupContext.readDecryptionResult(filename: String): Result<DecryptionResult, String> {
        return try {
            var proto: electionguard.protogen.DecryptionResult
            FileInputStream(filename).use { inp ->
                proto = electionguard.protogen.DecryptionResult.decodeFromStream(inp)
            }
            proto.import(this)
        } catch (e: Exception) {
            Err(e.message ?: "readDecryptionResult $filename failed")
        }
    }

    private class PlaintextBallotIterator(
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
                val ballot = ballotProto.import()
                if (filter != null && !filter.test(ballot)) {
                    continue // skip it
                }
                setNext(ballot)
                break
            }
        }
    }

    // Create iterators, so that we never have to read in all ballots at once.
    private class EncryptedBallotIterator(
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
                val ballotResult = ballotProto.import(groupContext)
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

    private class SpoiledBallotTallyIterator(
        filename: String,
        private val group: GroupContext,
    ) : AbstractIterator<DecryptedTallyOrBallot>() {
        private val input: FileInputStream = FileInputStream(filename)

        override fun computeNext() {
            val length = readVlen(input)
            if (length < 0) {
                input.close()
                return done()
            }
            val message = input.readNBytes(length)
            val tallyProto =
                electionguard.protogen.DecryptedTallyOrBallot.decodeFromByteBuffer(ByteBuffer.wrap(message))
            val tally = tallyProto.import(group)

            setNext(tally.getOrElse { throw RuntimeException("Tally failed to parse") })
        }
    }

    private fun GroupContext.readTrustee(filename: String): DecryptingTrustee {
        var proto: electionguard.protogen.DecryptingTrustee
        FileInputStream(filename).use { inp -> proto = electionguard.protogen.DecryptingTrustee.decodeFromStream(inp) }
        return proto.import(this).getOrElse { throw RuntimeException("DecryptingTrustee $filename failed to parse") }
    }

}

// variable length (base 128) int32
fun readVlen(input: InputStream): Int {
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
