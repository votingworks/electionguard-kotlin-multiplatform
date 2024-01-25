package electionguard.core

// Simultaneous Multiple exponentiation. Experimental.
// Algorithm 14.88 in Handbook (menezes et al)
private val debug = false
private val debug2 = false
private val show = false

class SMexp(val group: GroupContext, bases: List<ElementModP>, exps: List<ElementModQ>) {
    // there are k exponents which are integers of bitlength t = actualBitLength
    val k = exps.size
    val EAmatrix = BitMatrix(exps.map { it.byteArray() })
    val actualBitLength = EAmatrix.firstNonZeroColumn()

    val Gp : Map<BitMatrix.ColVector, ElementModP>

    init {
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
            val colv = EAmatrix.colVector(vecIdx)
            val factor: ElementModP = Gp[colv]!!
            result = result * result // square
            countSquare++
            if (factor != group.ONE_MOD_P) {
                result *= factor
                countMultiply++
            }
        }
        if (show) println("countSquare=$countSquare countMultiply=$countMultiply")
        return result
    }
}

private val byteZ = 0.toByte()

// bits are indexed from right to left. bigendian
class BitMatrix(val bas: List<ByteArray>) {
    val nrows = bas.size
    val byteWidth = bas[0].size
    val bitWidth = byteWidth * 8

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