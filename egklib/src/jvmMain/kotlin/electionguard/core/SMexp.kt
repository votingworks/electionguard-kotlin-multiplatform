package electionguard.core

import electionguard.json2.import
import java.math.BigInteger
import java.util.*
import kotlin.math.max
import kotlin.math.min

// Simultaneous Multiple exponentiation. Experimental.
// Algorithm 14.88 in Handbook (menezes et al)
//

private val bitlength = 256 // t
private val debug = false

class SMexp(val group: GroupContext, bases: List<ElementModP>, exps: List<ElementModQ>) {
    // there are k exps which are integers of bitlength t
    val k = exps.size
    val actualBitLength: Int

    val IAQ: List<Int>
    val IAQset: Set<Int>
    val G: Map<Int, ElementModP> // do we really need more than a long ? may be up to 2^nrows

    init {
        // form the k by t exponent array EA whose rows are the binary representations of the es
        val EA = exps.map { it.byteArray() } // big endian byte arrays

        // IA_j is the jth column vector of EA, low order bits are at the top of the column
        val IA32 = MutableList(bitlength) { colIdx ->
            val vec = mutableListOf<Int>()

            val EAbitpos = bitlength - 1 - colIdx // low order bits first
            val byteIdx = (EAbitpos) / 8
            val bitIdx = (EAbitpos) % 8
            val bitShift = 7 - bitIdx
            repeat(k) { rowIdx ->
                val b: Int = EA[rowIdx][byteIdx].toInt()
                val bit = (b shr bitShift) and 1
                vec.add(bit)
            }
            vec
            // group.binaryToElementModQ(ba)
        }.reversed()

        // find out the actual bit length
        var discard = 0
        for (idx in 0..bitlength) {
            discard = idx
            if (nonzero(IA32[idx])) break
        }
        actualBitLength = bitlength - discard
        val IA = IA32.subList(discard, bitlength)
        println("size of IA = ${IA.size}")

        val IAstring = buildString {
            appendLine("IA array")
            IA.forEachIndexed { idx, it ->
                appendLine(" $idx $it")
            }
        }
        if (debug) println(IAstring)

        IAQ = IA.mapIndexed { idx, it ->
            makeIAvalue(it, idx)
        }

        /*
        IAQ = IA.mapIndexed { idx, it ->
            val iarr = IntArray(32)
            var byteIndex = 31
            var bitIndex = 0
            it.forEach {
                val bitValue = it shl bitIndex
                iarr[byteIndex] += bitValue
                bitIndex++
                if (bitIndex == 8) {
                    byteIndex--
                    bitIndex = 0
                }
            }
            val ba = ByteArray(32) { iarr[it].toByte() }
            val q = group.binaryToElementModQ(ba)
            val asInt = (q as ProductionElementModQ).element.toInt()
            val makeInt = makeIAvalue(it, idx)
            require(makeInt == asInt)
            makeInt
        }

         */
        IAQset = HashSet(IAQ)
        println("size of IAQset = ${IAQset.size}")

        G = mutableMapOf()
        val nentries = 2 shl k - 1
        repeat(nentries) {
            if (IAQset.contains(it)) {
                var idx = it
                var pos = 0
                var accum = group.ONE_MOD_P
                while (idx > 0) {
                    val bit = idx and 1
                    if (bit != 0) {
                        accum = accum * bases[pos]
                    }
                    idx = idx shr 1
                    pos++
                }
                G[it] = accum
            }
        }
        println("size of G = ${G.size}")
    }

    fun prodPowP(): ElementModP {
        var result = group.ONE_MOD_P
        repeat(actualBitLength) { idx ->
            val ia = IAQ[idx]
            if (G[ia] == null) println("HEY no G for $ia, idx = $idx")
            val factor = G[ia]!! // LOOK
            result = result * result // square
            result = result * factor
        }
        return result
    }

    fun makeIAvalue(bits: List<Int>, idx: Int): Int {
        var result = 0
        bits.forEachIndexed { idx, it ->
            if (it == 1) {
                result += it shl idx
            }
        }
        if (debug) println("value for $idx is $result")
        return result
    }

    fun nonzero(bits: List<Int>): Boolean {
        bits.forEach {
            if (it == 1) {
                return true
            }
        }
        return false
    }
}