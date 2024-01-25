package electionguard.core

// Simultaneous Multiple exponentiation. Experimental.
// Algorithm 14.88 in Handbook (menezes et al)
//

private val bitlength = 256 // t
private val debug = false
private val debug1 = false
private val debug2 = false
private val show = false

class SMexp(val group: GroupContext, bases: List<ElementModP>, exps: List<ElementModQ>) {
    // there are k exps which are integers of bitlength t = actualBitLength
    val k = exps.size
    val EAmatrix = BitMatrix(exps.map { it.byteArray() })
    val actualBitLength = EAmatrix.firstNonZeroColumn()

    val Gp : Map<BitMatrix.ColVector, ElementModP>

    init {
        /* test
        repeat(actualBitLength) { colIdx ->
            val vecIdx = actualBitLength - colIdx - 1 // idx reversed
            val colbv = EAmatrix.colByteVector(vecIdx)
            val colv = EAmatrix.colVector(vecIdx)
            repeat(k) { rowIdx ->
                require (colv.isBitSet(rowIdx) == colbv.isBitSet(rowIdx))
            }
        } */

        var countMultiply = 0
        Gp = mutableMapOf()
        repeat(actualBitLength) { colIdx ->
            val vecIdx = actualBitLength - colIdx - 1 // idx reversed
            val colv = EAmatrix.colVector(vecIdx) // will be length k
            var accum = group.ONE_MOD_P
            repeat(k) { rowIdx ->
                if (colv.isBitSet(rowIdx)) {
                    accum = accum * bases[rowIdx]
                    countMultiply++
                    if (debug2) println(" $vecIdx accum for base $rowIdx")
                }
                Gp[colv] = accum
                // if (accum == group.ONE_MOD_P) println(" ONE at colIdx=$colIdx for base $rowIdx")
            }
        }
        if (debug) println("size of Gp = ${Gp.size}")
        if (show) println("precompute countMultiply=$countMultiply")
    }

    fun prodPowP(show: Boolean = false): ElementModP {
        var countSquare = 0
        var countMultiply = 0
        var result = group.ONE_MOD_P
        repeat(actualBitLength) { colIdx ->
            val vecIdx = actualBitLength - colIdx - 1 // idx reversed
            val colv = EAmatrix.colVector(vecIdx) // will be length k
            val factor: ElementModP = Gp[colv]!!
            result = result * result // square
            if (factor != group.ONE_MOD_P) {
                result = result * factor
                countMultiply++
            }
            countSquare++
        }
        if (show) println("countSquare=$countSquare countMultiply=$countMultiply")
        return result
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

    fun colByteVector(colidx: Int, bitSize: Int = nrows) : ByteVector {
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

    // here we use position in the underlying byte arrays
    private fun isBitSet(rowIdx: Int, bytePos: Int, bitPos: Int): Boolean {
        val ba = bas[rowIdx]
        val byteAsInt = ba[bytePos].toInt()
        val bit = byteAsInt shr bitPos and 1
        return bit != 0
    }

    fun colVector(colidx: Int) : ColVector {
        return ColVector(colidx)
    }

    // virtual Column Vector
    inner class ColVector(val colIdx: Int) {
        val bitpos = bitWidth - 1 - colIdx // low order bits first
        val bytePos = (bitpos) / 8
        val bitPos = 7 - (bitpos) % 8

        fun isBitSet(rowIdx: Int): Boolean {
            return isBitSet(rowIdx, bytePos, bitPos)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ColVector
            return colIdx == other.colIdx
        }

        override fun hashCode(): Int {
            return colIdx
        }
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