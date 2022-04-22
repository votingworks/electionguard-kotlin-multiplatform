package electionguard.publish

import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.protoconvert.publishPlaintextBallot
import electionguard.protoconvert.publishSubmittedBallot
import io.ktor.utils.io.errors.*
import pbandk.encodeToStream
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.OutputStream

@Throws(IOException::class)
fun GroupContext.writeInvalidBallots(fileout: String, invalidBallots: List<PlaintextBallot>) {
        FileOutputStream(fileout).use { out ->
            for (ballot in invalidBallots) {
                val ballotProto = ballot.publishPlaintextBallot()
                writeDelimitedTo(ballotProto, out)
            }
            out.close()
        }
}

class SubmittedBallotSink(path: String) : SubmittedBallotSinkIF {
    val out: FileOutputStream = FileOutputStream(path)

    override fun writeSubmittedBallot(ballot: SubmittedBallot) {
        val ballotProto: pbandk.Message = ballot.publishSubmittedBallot()
        writeDelimitedTo(ballotProto, out)
    }

    override fun close() {
        out.close()
    }
}

fun writeDelimitedTo(proto: pbandk.Message, output: OutputStream) {
    val bb = ByteArrayOutputStream()
    proto.encodeToStream(bb)
    writeVlen(bb.size(), output)
    output.write(bb.toByteArray())
}

fun writeVlen(input: Int, output: OutputStream) {
    var value = input
    while (true) {
        if (value and 0x7F.inv() == 0) {
            output.write(value)
            return
        } else {
            output.write(value and 0x7F or 0x80)
            value = value ushr 7
        }
    }
}
