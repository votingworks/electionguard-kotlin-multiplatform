package publish

import electionguard.ballot.ElectionRecord
import electionguard.ballot.ElectionRecordAllData
import electionguard.ballot.PlaintextTally
import electionguard.ballot.SubmittedBallot
import electionguard.core.GroupContext
import electionguard.protoconvert.importElectionRecord
import electionguard.protoconvert.importPlaintextTally
import electionguard.protoconvert.importSubmittedBallot
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

class Consumer(val publisher: Publisher, val groupContext: GroupContext) {

    companion object {
        fun fromElectionRecord(electionRecordDir: String, groupContext: GroupContext): Consumer {
            return Consumer(Publisher(Path.of(electionRecordDir), Publisher.Mode.readonly), groupContext)
        }
    }

    @Throws(IOException::class)
    fun readElectionRecordAllData(): ElectionRecordAllData {
        val where = publisher.electionRecordProtoPath()
        val electionRecord: ElectionRecord?
        if (Files.exists(where)) {
            electionRecord = readElectionRecordProto()
        } else {
            throw FileNotFoundException("No election record found in $where")
        }
        if (electionRecord == null) {
            throw IOException("Malformed election record found in $where")
        }

        return ElectionRecordAllData(electionRecord, iterateSubmittedBallots(), emptyList())
    }

    @Throws(IOException::class)
    fun readElectionRecordProto(): ElectionRecord? {
        var proto: electionguard.protogen.ElectionRecord
        val filename = publisher.electionRecordProtoPath().toString()
        FileInputStream(filename).use { inp -> proto = electionguard.protogen.ElectionRecord.decodeFromStream(inp) }
        return proto.importElectionRecord(groupContext)
    }

    // all submitted ballots cast or spoiled
    fun iterateSubmittedBallots(): Iterable<SubmittedBallot> {
        if (!Files.exists(publisher.submittedBallotProtoPath())) {
            return emptyList()
        }
        return SubmittedBallotIterable { true }
    }

    // all submitted ballots cast only
    fun iterateCastBallots(): Iterable<SubmittedBallot> {
        if (!Files.exists(publisher.submittedBallotProtoPath())) {
            return emptyList()
        }
        return SubmittedBallotIterable { it.state == electionguard.protogen.SubmittedBallot.BallotState.CAST }
    }

    // all spoiled ballots spoiled only
    fun iterateSpoiledBallots(): Iterable<SubmittedBallot> {
        if (!Files.exists(publisher.submittedBallotProtoPath())) {
            return emptyList()
        }
        return SubmittedBallotIterable { it.state == electionguard.protogen.SubmittedBallot.BallotState.SPOILED }
    }

    // all spoiled ballot tallies
    fun iterateSpoiledBallotTallies(): Iterable<PlaintextTally> {
        if (!Files.exists(publisher.spoiledBallotProtoPath())) {
            return emptyList()
        }
        return SpoiledBallotTallyIterable()
    }

    // TODO figure out how to use SAM
    private inner class SubmittedBallotIterable(val filter: Predicate<electionguard.protogen.SubmittedBallot>) :
        Iterable<SubmittedBallot> {
        override fun iterator(): Iterator<SubmittedBallot> {
            return SubmittedBallotIterator(publisher.submittedBallotProtoPath().toString(), filter)
        }
    }

    private inner class SpoiledBallotTallyIterable : Iterable<PlaintextTally> {
        override fun iterator(): Iterator<PlaintextTally> {
            return SpoiledBallotTallyIterator(publisher.spoiledBallotProtoPath().toString())
        }
    }

    // Create iterators, so that we never have to read in all ballots at once.
    // Making them Closeable makes sure that the FileInputStream gets closed.
    // use in a try-with-resources block
    private inner class SubmittedBallotIterator(
        filename: String,
        val filter: Predicate<electionguard.protogen.SubmittedBallot>,
    ) : AbstractIterator<SubmittedBallot>() {

        private val input: FileInputStream

        init {
            this.input = FileInputStream(filename)
        }

        override fun computeNext() {
            val length = readVlen(input)
            if (length < 0) {
                input.close()
                return done()
            }
            val message = input.readNBytes(length)
            val ballotProto = electionguard.protogen.SubmittedBallot.decodeFromByteBuffer(ByteBuffer.wrap(message))
            if (!filter.test(ballotProto)) {
                computeNext() // LOOK fix recursion
                return
            }
            val ballot = ballotProto.importSubmittedBallot(groupContext) ?: throw RuntimeException("Ballot didnt parse")
            setNext(ballot)
        }
    }

    private inner class SpoiledBallotTallyIterator(
        filename: String,
    ) : AbstractIterator<PlaintextTally>() {

        private val input: FileInputStream

        init {
            this.input = FileInputStream(filename)
        }

        override fun computeNext() {
            val length = readVlen(input)
            System.out.printf("readVlen %d%n", length)
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