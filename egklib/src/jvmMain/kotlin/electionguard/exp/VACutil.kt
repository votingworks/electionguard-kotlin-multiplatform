package electionguard.exp

import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.normalize
import org.cryptobiotic.bigint.BigInteger

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

    fun countBits(bitLength: Int): Int {
        var count = 0
        repeat(bitLength) {
            count += colVector(it).countBits()
        }
        return count
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

        fun countBits(): Int {
            var count = 0
            repeat(nrows) {
                if (isBitSet(it)) count++
            }
            return count
        }

        fun toInt(): Int {
            var result = 0
            repeat(nrows) {
                if (isBitSet(it)) result = result + (1 shl it)
            }
            return result
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

typealias ColVector = Int

// The column vectors are vectors of 0, 1.
// We can use Int for vectors up to 32. We'll see if we need more dimensions
// it may be better to keep the dimensionality low, breaking problem up for performance reasons.
// k is the number of exponents (nrows, eg ballots).
class CVInt(val orgIdx: Int, val cv: ColVector) : Comparable<CVInt> {
    var sum2: Pair<ColVector, ColVector>? = null // not indexes,  these are the column vector values

    constructor(nlist: List<Int>) : this(0, toIntN(nlist))

    fun countOneBits() = cv.countOneBits()
    fun done() = (sum2 != null)
    fun setSum(i1: ColVector, i2: ColVector): CVInt { sum2 = Pair(i1, i2); return this }
    fun toNList() = cv.toNList()
    fun toVector(k: Int) = cv.toVector(k)

    override fun compareTo(other: CVInt): Int {
        return this.cv - other.cv
    }

    override fun toString() = buildString {
        append(" CVInt(orgIdx=$orgIdx, cv=$cv, bitsOn=${cv.countOneBits()}")
        if (sum2 != null) append(", sum=$sum2")
        append(")")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CVInt
        return cv == other.cv
    }

    override fun hashCode(): Int {
        return cv
    }

}


////////////////////////////////////////////////////////////////////////////////

// VAC's may have any value in their elements, ie not just 0 or 1
// addition chains need this capability
class VAC(val k: Int, val elems: IntArray = IntArray(k)) {

    constructor(k: Int) : this(k, 0)

    constructor(k: Int, cv: Int) : this(k, cv.toVector(k))

    fun isEmpty(): Boolean {
        elems.forEach { if (it != 0) return false }
        return true
    }

    fun square() : VAC {
        val result = IntArray(k) { 2 * elems[it] }
        return VAC(k, result)
    }

    fun product(v: List<Int>): VAC {
        val result = IntArray(k) { elems[it] + v[it] }
        return VAC(k, result)
    }

    fun product(other: IntArray): VAC {
        val result = IntArray(k) { elems[it] + other[it] }
        return VAC(k, result)
    }

    fun contains(v: List<Int>): Boolean {
        v.forEach {
            if (elems[it] == 0) return false
        }
        return true
    }

    override fun toString(): String {
        return " ${elems.contentToString()}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VAC
        return elems.contentEquals(other.elems)
    }

    override fun hashCode(): Int {
        return elems.contentHashCode()
    }
}

/////////////////////////////////////////////////////////////////////////////

// The column vectors are vectors of {0,1} .
class BitVector(val k: Int, val elems: IntArray, val cv: Int = elems.toCV()) : Comparable<BitVector> {
    // hmm, i dont know if this should be here
    var sum2: Pair<ColVector, ColVector>? = null

    init {
        require (k == elems.size)
        elems.forEach{ require ((it == 0) || (it == 1)) }
    }

    constructor(k: Int, cv: Int) : this(k, cv.toVector(k))
    constructor(k: Int, blist: List<Int>) : this(k, binary(k, blist))
    fun setSum(i1: ColVector, i2: ColVector): BitVector { sum2 = Pair(i1, i2); return this } // LOOK
    fun countOneBits() = cv.countOneBits()

    fun isEmpty(): Boolean {
        return cv == 0
    }

    fun xor(other: BitVector) = BitVector(k, elems.mapIndexed{ idx, it -> it xor other.elems[idx] }.toVector() )

    fun isSumOf(a: BitVector, b: BitVector): Boolean {
        return this.equals(a.xor(b))
    }

    // is this one bit different? yes, return bit position, else null
    fun isOneBitDifferent(a: BitVector): Int? {
        val diff = this.xor(a)
        return diff.isOneBit()
    }

    // is this a one bit value? yes, return bit position, else null
    fun isOneBit(): Int? {
        return if (countOneBits() == 1) {
            var found: Int? = null
            for (i in 0 until k) {
                if (elems[i] == 1) { found = i; break }
            }
            found
        } else null
    }

    fun divide(): Pair<BitVector, BitVector> {
        val nonzero = cv.toNList()
        val size2 = nonzero.size / 2
        val left = nonzero.subList(0, size2)
        val right = nonzero.subList(size2, nonzero.size)
        val left2 = BitVector(k, binary(k, left))
        val right2 = BitVector(k, binary(k, right))
        return Pair(left2, right2)
    }

    override fun compareTo(other: BitVector): Int {
        return this.cv - other.cv
    }

    override fun toString(): String {
        return " $cv: ${elems.contentToString()}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BitVector
        return elems.contentEquals(other.elems)
    }

    override fun hashCode(): Int {
        return elems.contentHashCode()
    }
}


// convert to list of indices that are 1
fun Int.toNList(): List<Int> {
    val result = mutableListOf<Int>()
    var work = this
    var bit = 0
    while (work != 0) {
        if (work and 1 == 1) result.add(bit)
        work = work shr 1
        bit++
    }
    return result
}

// reverse of toNList()
fun toIntN(nonzero: List<Int>): Int {
    var result = 0
    nonzero.forEachIndexed { idx, it ->
        result = result + (1 shl it)
    }
    return result
}

// to vector of 0, 1
fun Int.toVector(k: Int): IntArray {
    val nlist = this.toNList()
    val elems = IntArray(k)
    nlist.forEach { elems[it] = 1 }
    return elems
}

// inverse of Int.toVector()
fun IntArray.toCV(): Int {
    val k = this.size
    var work = 0
    for (j in k-1 downTo 0) {
        work = work shl 1
        if (this[j] != 0) work++
    }
    return work
}

// from vector to nlist
fun nonzero(vector: IntArray): List<Int> {
    val result = mutableListOf<Int>()
    vector.forEachIndexed { idx, it -> if (it != 0) result.add(idx) }
    return result
}

// from nlist to vector
fun binary(k: Int, nonzero: List<Int>): IntArray {
    val result = IntArray(k)
    nonzero.forEach { result[it] = 1 }
    return result
}

// from blist to vector
fun List<Int>.toVector(): IntArray {
    return IntArray(this.size) { this[it] }
}

// from vector to Blist
fun IntArray.toBlist(): List<Int> {
    return List(this.size) { this[it] }
}

fun List<Int>.toCV(): Int {
    return this.toVector().toCV()
}

fun BigInteger.toByteArray(width: Int): ByteArray {
    return this.toByteArray().normalize(width)
}

fun ElementModP.toBig(): BigInteger {
    return BigInteger(1, this.byteArray().normalize(512))
}

fun ElementModQ.toBig(): BigInteger {
    return BigInteger(1, this.byteArray().normalize(32))
}

fun ElementModP.toBigM(): java.math.BigInteger {
    return java.math.BigInteger(1, this.byteArray().normalize(512))
}

fun ElementModQ.toBigM(): java.math.BigInteger {
    return java.math.BigInteger(1, this.byteArray().normalize(512))
}