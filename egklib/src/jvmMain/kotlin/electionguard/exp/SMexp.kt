package electionguard.exp

// Simultaneous Multiple exponentiation. Experimental.
// Algorithm 14.88 in Handbook (menezes et al)
private val debug = true
private val debug1 = true
private val debug2 = false
private val showSM = true

class SMexp(val group: GroupContext, bases: List<ElementModP>, exps: List<ElementModQ>) {
    // there are k exponents which are integers of bitlength t = actualBitLength
    val k = exps.size
    val EAmatrix = BitMatrix(exps.map { it.byteArray() })
    val actualBitLength = EAmatrix.firstNonZeroColumn()

    val Gp : Map<BitMatrix.ColVector, ElementModP>

    init {
        if (debug1) println("EAmatrix bitLength=$actualBitLength, bitCount=${EAmatrix.countBits(actualBitLength)}")

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
        if (showSM) println("precompute countMultiply=$countMultiply")
    }

    fun prodPowP(show: Boolean = false): ElementModP {
        var countSquare = 0
        var countMultiply = 0
        var result = group.ONE_MOD_P
        repeat(actualBitLength) { colIdx ->
            // first time dont square
            if (result != group.ONE_MOD_P) {
                result = result * result // square
                countSquare++
            }

            val vecIdx = actualBitLength - colIdx - 1 // idx reversed
            val colv = EAmatrix.colVector(vecIdx)
            val factor: ElementModP = Gp[colv]!!

            // heres where we need the high bit, or you could say we ignore everything until we find it
            if (factor != group.ONE_MOD_P) {
                result *= factor
                countMultiply++
            }

        }
        if (show) println("countSquare=$countSquare countMultiply=$countMultiply")
        return result
    }
}