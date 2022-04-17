package electionguard.publish

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getErrorOr
import com.github.michaelbull.result.unwrap
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
import mu.KotlinLogging
import pbandk.decodeFromByteBuffer
import pbandk.decodeFromStream
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Predicate

internal val logger = KotlinLogging.logger("Consumer")

actual class Consumer actual constructor(topDir: String, val groupContext: GroupContext) {
    val path = ElectionRecordPath(topDir)

    @Throws(IOException::class)
    actual fun readElectionRecordAllData(): ElectionRecordAllData {
        val where = path.electionRecordProtoPath()
        val electionRecord: ElectionRecord?
        if (Files.exists(Path.of(where))) {
            electionRecord = readElectionRecord()
        } else {
            throw FileNotFoundException("No election record found in $where")
        }

        return ElectionRecordAllData(
            electionRecord.protoVersion,
            electionRecord.constants,
            electionRecord.manifest,
            electionRecord.context ?: throw RuntimeException("missing context"),
            electionRecord.guardianRecords ?: emptyList(),
            electionRecord.devices ?: emptyList(),
            electionRecord.encryptedTally ?: throw RuntimeException("missing encryptedTally"),
            electionRecord.decryptedTally ?: throw RuntimeException("missing decryptedTally"),
            electionRecord.availableGuardians ?: emptyList(),
            iterateSubmittedBallots(),
            iterateSpoiledBallotTallies(),
        )
    }

    @Throws(IOException::class)
    actual fun readElectionRecord(): ElectionRecord {
        var proto: electionguard.protogen.ElectionRecord
        val filename = path.electionRecordProtoPath()
        FileInputStream(filename).use { inp -> proto = electionguard.protogen.ElectionRecord.decodeFromStream(inp) }
        return proto.importElectionRecord(groupContext)
    }

    // all plaintext ballots
    actual fun iteratePlaintextBallots(ballotDir: String): Iterable<PlaintextBallot> {
        if (!Files.exists(Path.of(path.plaintextBallotProtoPath(ballotDir)))) {
            return emptyList()
        }
        return Iterable { PlaintextBallotIterator(path.plaintextBallotProtoPath(ballotDir)) }
    }

    private inner class PlaintextBallotIterator(
        filename: String,
    ) : AbstractIterator<PlaintextBallot>() {

        private val input: FileInputStream = FileInputStream(filename)

        override fun computeNext() {
            val length = readVlen(input)
            if (length < 0) {
                input.close()
                return done()
            }
            val message = input.readNBytes(length)
            val ballotProto = electionguard.protogen.PlaintextBallot.decodeFromByteBuffer(ByteBuffer.wrap(message))
            val ballot = ballotProto.importPlaintextBallot()
            setNext(ballot)
        }
    }

    // all submitted ballots, cast or spoiled
    actual fun iterateSubmittedBallots(): Iterable<SubmittedBallot> {
        if (!Files.exists(Path.of(path.submittedBallotProtoPath()))) {
            return emptyList()
        }
        return Iterable { SubmittedBallotIterator({ true }) }
    }

    // only cast SubmittedBallots
    actual fun iterateCastBallots(): Iterable<SubmittedBallot> {
        if (!Files.exists(Path.of(path.submittedBallotProtoPath()))) {
            return emptyList()
        }
        val filter =
            Predicate<electionguard.protogen.SubmittedBallot> { it.state == electionguard.protogen.SubmittedBallot.BallotState.CAST }
        return Iterable { SubmittedBallotIterator(filter) }
    }

    // only spoiled SubmittedBallots
    actual fun iterateSpoiledBallots(): Iterable<SubmittedBallot> {
        if (!Files.exists(Path.of(path.submittedBallotProtoPath()))) {
            return emptyList()
        }
        val filter =
            Predicate<electionguard.protogen.SubmittedBallot> { it.state == electionguard.protogen.SubmittedBallot.BallotState.SPOILED }
        return Iterable { SubmittedBallotIterator(filter) }
    }

    // Create iterators, so that we never have to read in all ballots at once.
    private inner class SubmittedBallotIterator(
        val filter: Predicate<electionguard.protogen.SubmittedBallot>,
    ) : AbstractIterator<SubmittedBallot>() {

        private val input: FileInputStream = FileInputStream(path.submittedBallotProtoPath())

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
                    logger.warn { ballotResult.getErrorOr("Unknown error on ${ballotProto.ballotId}")}
                    continue
                }
            }
        }
    }

    // all spoiled ballot tallies
    actual fun iterateSpoiledBallotTallies(): Iterable<PlaintextTally> {
        if (!Files.exists(Path.of(path.spoiledBallotProtoPath()))) {
            return emptyList()
        }
        return Iterable { SpoiledBallotTallyIterator(path.spoiledBallotProtoPath()) }
    }

    private inner class SpoiledBallotTallyIterator(
        filename: String,
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
            val tally = tallyProto.importPlaintextTally(groupContext) ?: throw RuntimeException("Tally didnt parse")
            setNext(tally)
        }
    }

    actual fun readTrustees(trusteeDir: String): List<DecryptingTrusteeIF> {
        val trusteeDirPath = Path.of(trusteeDir)

        if (!Files.exists(trusteeDirPath) || !Files.isDirectory(trusteeDirPath)) {
            return emptyList()
        }
        val result = ArrayList<DecryptingTrusteeIF>()
        for (filename in trusteeDirPath.toFile().listFiles()!!) {
            // TODO can we screen out bad files?
            val trusteeProto = readTrusteeProto(filename.absolutePath)
            if (trusteeProto != null) {
                result.add(trusteeProto.importDecryptingTrustee(groupContext))
            }
        }
        return result
    }

    fun readTrusteeProto(filename: String): electionguard.protogen.DecryptingTrustee? {
        var trusteeProto: electionguard.protogen.DecryptingTrustee? = null
        FileInputStream(filename).use { input ->
            val length = readVlen(input)
            if (length > 0) {
                val message = input.readNBytes(length)
                trusteeProto =
                    electionguard.protogen.DecryptingTrustee.decodeFromByteBuffer(ByteBuffer.wrap(message))
            }
        }
        return trusteeProto
    }
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