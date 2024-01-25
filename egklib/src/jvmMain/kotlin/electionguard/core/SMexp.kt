package electionguard.core

// Simultaneous Multiple exponentiation. Experimental.
// Algorithm 14.88 in Handbook (menezes et al)
//

private val bitlength = 256 // t
private val debug = false
private val debug1 = false
private val debug2 = false

class SMexp(val group: GroupContext, bases: List<ElementModP>, exps: List<ElementModQ>) {
    // there are k exps which are integers of bitlength t
    val k = exps.size
    val actualBitLength: Int

    val IAQ: List<ByteVector> // needs to be BigInteger, need a bit for every row.
    val G: Map<ByteVector, ElementModP>

    val EAmatrix = BitMatrix(exps.map { it.byteArray() })
    val Gp : Map<ByteVector, ElementModP>

    init {
        // form the k by t exponent array EA whose rows are the binary representations of the e's
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
        val test = EAmatrix.firstNonZeroColumn()
        if (debug) println("runSM for nrows = $k, nbits=$actualBitLength alt = $test")
        if (actualBitLength != test) {
            EAmatrix.firstNonZeroColumn()
        }
        require(actualBitLength == test)

        val IA = IA32.subList(discard, bitlength)
        if (debug) println(" size of IA = ${IA.size}")

        val nbytes = (k + 7) / 8
        IAQ = IA.mapIndexed { idx, it ->
            val iarr = IntArray(nbytes)
            var byteIndex = nbytes-1
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
            val ba = ByteArray(nbytes) { iarr[it].toByte() }
            ByteVector(ba, k)
        }


        if (debug2) {
            val IAstring = buildString {
                appendLine("IA array")
                IA.forEachIndexed { idx, it ->
                    val itq = IAQ[idx]
                    appendLine(" $idx $it = ${itq.toInt()}")
                }
            }
            println(IAstring)
        }

        if (debug1) {
            repeat(actualBitLength) { colIdx ->
                val colVector = EAmatrix.colVector(colIdx) // idx reversed
                println("$colIdx colVector = ${colVector.toInt()}")
            }
        }


        G = mutableMapOf()
        IAQ.forEachIndexed { idx, bv ->
            var pos = 0
            var accum = group.ONE_MOD_P
            while (pos < k) {
                if (bv.isBitSet(pos)) {
                    accum = accum * bases[pos]
                    if (debug2) println(" $idx accum for base $pos")
                }
                pos++
            }
            G[bv] = accum
            // if (accum == group.ONE_MOD_P) println(" ONE $idx accum for base $pos")
        }
        if (debug) println("size of G = ${G.size}")

        Gp = mutableMapOf()
        repeat(actualBitLength) { colIdx ->
            val vecIdx = actualBitLength - colIdx - 1 // idx reversed
            val colv = EAmatrix.colVector(vecIdx) // will be length k
            var accum = group.ONE_MOD_P
            repeat(k) { rowIdx ->
                if (colv.isBitSet(rowIdx)) {
                    accum = accum * bases[rowIdx]
                    if (debug2) println(" $vecIdx accum for base $rowIdx")
                }
                Gp[colv] = accum
                // if (accum == group.ONE_MOD_P) println(" ONE at colIdx=$colIdx for base $rowIdx")
            }

        }
        if (debug) println("size of Gp = ${Gp.size}")
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

    fun prodPowP2(): ElementModP {
        var result = group.ONE_MOD_P
        repeat(actualBitLength) { colIdx ->
            val vecIdx = actualBitLength - colIdx - 1 // idx reversed
            val colv = EAmatrix.colVector(vecIdx) // will be length k
            val factor: ElementModP = Gp[colv]!!
            result = result * result // square
            if (colv.nonzero()) {
                result = result * factor
            }
        }
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

private val byteZ = 0.toByte()

// main point of this is to make hashing work
class ByteVector(val ba : ByteArray, val bitSize:Int = ba.size * 8) {
    val byteWidth = ba.size
    val bitWidth = byteWidth * 8

    // bits are indexed from right to left.
    fun isBitSet(colIdx: Int): Boolean {
        val bitpos = bitWidth - 1 - colIdx // low order bits first
        val byteidx = (bitpos) / 8
        val bitIdx = (bitpos) % 8
        val bitShift = 7 - bitIdx

        val byteAsInt = ba[byteidx].toInt()
        val isSet =  byteAsInt shr bitShift and 1 != 0
        return isSet
    }

    // assumes upper bits are zero
    fun nonzero(): Boolean {
        ba.forEach {
            if (it != byteZ) return true
        }
        return false
    }

    fun toInt(): Int {
        var result = 0
        repeat(bitSize) { colidx ->
            if (isBitSet(colidx)) {
                result += (1 shl colidx)
            }
        }
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ByteVector
        return ba.contentEquals(other.ba)
    }

    override fun hashCode(): Int {
        return ba.contentHashCode()
    }

}

// bits are indexed from right to left. bigendian
class BitMatrix(val bas: List<ByteArray>) {
    val nrows = bas.size
    val byteWidth = bas[0].size
    val bitWidth = byteWidth * 8

    fun colVector(colidx: Int, bitSize: Int = nrows) : ByteVector {
        val byteSize = (bitSize + 7) / 8
        val result = ByteArray(byteSize)

        val (byteidx, bitidx) = pos(colidx, bitWidth)
        val bitShift = 7 - bitidx

        repeat(nrows) { rowIdx ->
            val ba = bas[rowIdx]
            val b: Int = ba[byteidx].toInt()
            val bit = (b shr bitShift) and 1
            if (bit != 0) {
                val (colbyte, colbit) = pos(rowIdx, byteSize * 8)
                val bitShift = 7 - colbit
                result[colbyte] = (result[colbyte].toInt() + (1 shl bitShift)).toByte()
            }
        }
        return ByteVector(result, bitSize)
    }

    fun firstNonZeroColumn(): Int {
        var bytePos = 0
        while (bytePos < byteWidth) {
            var allzero = true
            repeat (nrows) { rowIdx ->
                allzero = allzero &&  (bas[rowIdx][bytePos] == byteZ)
            }
            if (!allzero) break
            bytePos++
        }

        var bitPos = 7
        while (bitPos >= 0) {
            var allzero = true
            repeat (nrows) { rowIdx ->
                allzero = allzero && !isBitSet(rowIdx, bytePos, bitPos)
            }
            if (!allzero) break
            bitPos--
        }

        // col index from the right
        val b1 =  bitWidth - (bytePos+1) * 8
        return b1 + bitPos + 1
    }

    private fun isBitSet(rowIdx: Int, bytePos: Int, bitPos: Int): Boolean {
        val ba = bas[rowIdx]
        val byteAsInt = ba[bytePos].toInt()
        val bit = byteAsInt shr bitPos and 1
        return bit != 0
    }
}

// convert from colIdx to byteIndex, bitIndex
// bitWidth if the storage
fun pos(colIdx: Int, bitWidth: Int): Pair<Int, Int> {
    val bitpos = bitWidth - 1 - colIdx
    val byteidx = (bitpos) / 8
    val bitidx = (bitpos) % 8 // maybe should be 7 - bitIdx ?
    return Pair(byteidx, bitidx)
}