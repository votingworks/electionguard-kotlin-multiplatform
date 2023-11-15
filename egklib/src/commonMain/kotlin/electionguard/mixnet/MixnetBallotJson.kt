package electionguard.mixnet

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.core.Base16.fromHex
import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.core.fileReadText
import electionguard.util.Indent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MixnetBallotJson(
    val wtf : List<List<List<String>>>,
) {
    fun show(): String{
        return buildString {
            val indent = Indent(0)
            wtf.forEachIndexed { idx1, it1 ->
                appendLine("${indent}ballot-${idx1+1} [")
                val indent1 = indent.incr()
                it1.forEachIndexed { idx2, it2 ->
                    val what = if (idx2 == 0) "pad" else "data"
                    appendLine("${indent1}${what} [")
                    val indent2 = indent1.incr()
                    it2.forEachIndexed { idx3, it3 ->
                        appendLine("$indent2 ${idx3+1} ${it3.substring(2, 20)}...")
                    }
                    appendLine("$indent1]")
                }
                appendLine("$indent]")
            }
        }
    }
}

data class MixnetBallot(
    val ciphertext: List<ElGamalCiphertext>
) {
    fun removeFirst() : MixnetBallot = MixnetBallot(ciphertext.subList(1, ciphertext.size))

    fun show(): String{
        return buildString {
            ciphertext.forEachIndexed { idx, it ->
                appendLine("${idx+1} $it")
            }
        }
    }
}

fun MixnetBallotJson.import(group: GroupContext) : List<MixnetBallot> {
    val mxBallots =
        wtf.map { padAndData ->
            val pads = padAndData[0].map { it.convertFromMixnetString(group) }
            val datas = padAndData[1].map { it.convertFromMixnetString(group) }
            val ciphers = pads.zip(datas).map { (pad, data) -> ElGamalCiphertext(pad, data )}
            MixnetBallot(ciphers)
        }
    return mxBallots
}

fun readMixnetJsonBallots(group: GroupContext, filename: String): List<MixnetBallot> {
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true }
    val result = readMixnetBallotArray(jsonReader, filename)
    return result.unwrap().import(group)
}

private fun readMixnetBallotArray(jsonReader: Json, filename: String): Result<MixnetBallotJson, String> =
    try {
        val text = fileReadText(filename)
        val lists = jsonReader.decodeFromString<List<List<List<String>>>>(text)
        val mixnetBallotJson = MixnetBallotJson(lists)
        Ok(mixnetBallotJson)
    } catch (e: Exception) {
        e.printStackTrace()
        Err(e.message ?: "readMixnetInput on $filename error")
    }

/////////////////////////////////////////////////////////////////////////////////////////

fun List<MixnetBallot>.publishJson() : List<List<List<String>>> {
    val wtf = mutableListOf<List<List<String>>>()

    this.forEach { mixnetBallot ->
        val pads = mutableListOf<String>()
        val data = mutableListOf<String>()
        mixnetBallot.ciphertext.forEach {
            pads.add( it.pad.convertToMixnetString() )
            data.add( it.data.convertToMixnetString() )
        }
        wtf.add(listOf(pads, data))
    }

    return wtf
}

private fun ElementModP.convertToMixnetString() : String {
    return "00" + this.toHex().lowercase()
}

private fun String.convertFromMixnetString(group: GroupContext) : ElementModP {
    val strip00 = this.substring(2).uppercase()
    return group.binaryToElementModP(strip00.fromHex()!!)!!
}