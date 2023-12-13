package electionguard.ballot

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.core.safeEnumValueOf
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

///////////////////////////////////////////////////////////
// ad-hoc encoding

//    val overvotes: List<Int>,
//    val writeIns: List<String>,
//    val status: ContestDataStatus = if (overvotes.isNotEmpty()) ContestDataStatus.over_vote else ContestDataStatus.normal,
actual fun ContestData.encodeToByteArray(fill: String?): ByteArray {
    val bas = ByteArrayOutputStream()
    writeVlen(this.overvotes.size, bas)
    this.overvotes.forEach {
        writeVlen(it, bas)
    }
    writeVlen(this.writeIns.size, bas)
    this.writeIns.forEach {
        writeString(it, bas)
    }
    writeString(this.status.name, bas)
    // pad with zeros
    if (fill != null && fill.length > 0) {
        bas.write(ByteArray(fill.length))
    }
    return bas.toByteArray()
}

private fun writeVlen(input: Int, output: OutputStream) {
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
private fun writeString(input: String, output: OutputStream) {
    val ba = input.toByteArray()
    writeVlen(ba.size, output)
    output.write(ba)
}

actual fun ByteArray.decodeToContestData() : Result<ContestData, String> {
    val bas = ByteArrayInputStream(this)
    val novervotes = readVlen(bas)
    val overvotes = mutableListOf<Int>()
    repeat(novervotes) {
        val vote = readVlen(bas)
        overvotes.add(vote)
    }
    val nwriteins = readVlen(bas)
    val writeins = mutableListOf<String>()
    repeat(nwriteins) {
        val writein = readString(bas)
        writeins.add(writein)
    }
    val name = readString(bas)
    val status = safeEnumValueOf<ContestDataStatus>(name)?: ContestDataStatus.normal
    return Ok(ContestData(overvotes, writeins, status))
}

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
private fun readString(input: InputStream): String {
    val size = readVlen(input)
    val ba = ByteArray(size)
    input.read(ba)
    return String(ba)
}