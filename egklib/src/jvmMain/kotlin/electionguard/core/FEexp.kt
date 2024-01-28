package electionguard.core

import electionguard.util.sigfig

// Use 14.88 to generate a vector addition chain
// Then use Algorithm 14.104 in Handbook (menezes et al)
private val debug = false
private val debug1 = false
private val showProducts = true
private var showConstruction = false

class AdditionChainElem(val k: Int, val elems: IntArray = IntArray(k)) {
    var index: Int = -999

    constructor(k: Int, indices: List<Int>) : this(k) {
        indices.forEach { elems[it] = 1 }
    }

    fun clone() = AdditionChainElem(k, elems.clone())

    fun isEmpty(): Boolean {
        elems.forEach { if (it != 0) return false }
        return true
    }

    fun square() = AdditionChainElem(k, IntArray(k) { 2 * elems[it] })

    fun product(v: List<Int>): AdditionChainElem {
        val result = clone()
        v.forEach { result.elems[it]++ }
        return result
    }

    fun contains(v: List<Int>): Boolean {
        v.forEach {
            if (elems[it] == 0) return false
        }
        return true
    }

    override fun toString(): String {
        return " $index: ${elems.contentToString()}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AdditionChainElem
        return elems.contentEquals(other.elems)
    }

    override fun hashCode(): Int {
        return elems.contentHashCode()
    }
}

class Wentry(val index: Int, val i1: Int, val i2:Int, var refCount: Int = 1) {
    override fun toString(): String {
        return "Wentry($index, i1=$i1, i2=$i2, refCount=$refCount)"
    }
}

class VectorAdditionChain(val k: Int, val show: Boolean = false) {
    val chain = mutableListOf<AdditionChainElem>()
    val w = mutableListOf<Wentry>()
    val elemMap = mutableMapOf<AdditionChainElem, AdditionChainElem>()

    fun wadd(i1: Int, i2: Int, refcount: Int = 1) {
        w.add(Wentry(w.size, i1, i2, refcount))
    }

    fun addProducts(products: List<Product>) {
        products.forEach {
            val ac = AdditionChainElem(k, it.nonbinary)
            chain.add(ac)
            wadd(it.w.first, it.w.second, 0)
            // leave out the singletons, they should be found directly in the bases LOOK
            if (it.nonbinary.size > 1) elemMap[ac] = ac
            ac.index = chain.size - 1
        }
    }

    fun addSquare(result: AdditionChainElem): AdditionChainElem {
        val square = result.square()
        add(square)
        wadd(result.index, result.index)
        if (showConstruction)  println(" addSquare to get ${showLast()}")
        return square
    }

    fun addFirstFactor(v: List<Int>): AdditionChainElem {
        // assume we must already have this factor
        val factorIdx = findFactor(v)
        if (factorIdx != null) {
            val factorac = AdditionChainElem(k, v)
            wadd(SpecialOneFactor, factorIdx)
            add(factorac)
            if (showConstruction) println(" addFirstFactor to get ${showLast()}")
            return factorac
        } else {
            throw RuntimeException("addFirstFactor")
        }
    }

    fun addFactor(prev: AdditionChainElem, v: List<Int>): AdditionChainElem {
        val result = prev.product(v)

        // do we already have this result?
        val resultPrev = elemMap[result]
        if (resultPrev != null) {
            println("  ${w.size} already have this factor $resultPrev")
            return resultPrev
        }

        // assume we must already have this factor
        val factorIdx = findFactor(v)
        if (factorIdx != null) {
            wadd(prev.index, factorIdx)
        } else {
            println("  ${w.size} cant find factor $v")
            val factorac = AdditionChainElem(k, v)
            val wtf = elemMap[factorac]
            wadd(prev.index, -999) // throw Exception
        }

        add(result)
        if (showConstruction) println(" addFactor $v to get ${showLast()}")
        return result
    }

    fun findFactor(v: List<Int>): Int? {
        if (v.size == 1) {
            return -v[0]-1 // its a base
        }
        // otherwise search by hashcode
        val factorac = AdditionChainElem(k, v)
        return elemMap[factorac]?.index
    }

    fun search(v: List<Int>): Int {
        chain.forEachIndexed { idx, it ->
            if (it.contains(v)) return idx
        }
        return -999 // haha
    }

    private fun add(ac: AdditionChainElem) {
        chain.add(ac)
        elemMap[ac] = ac
        ac.index = chain.size - 1
    }

    fun showLast(): String {
        return "${chain[chain.size - 1]} ${w[chain.size - 1]}"
    }

    override fun toString() = buildString {
        appendLine("VectorAdditionChain")
        chain.forEachIndexed { idx, it ->
            append("  $it")
            if (idx < w.size) append("  ${w[idx]}")
            appendLine()
        }
    }

    fun countRef(): Int {
        w.forEach{
            if (it.i1 >= 0 ) w[it.i1].refCount++
            if (it.i2 >= 0 ) w[it.i2].refCount++
        }

        var notmissing = 0
        w.forEach{
            if (it.refCount > 0 ) notmissing++
        }
        return notmissing
    }
}

data class Product(val nonbinary: List<Int>, val w: Pair<Int, Int>)

class KProducts(val k: Int, val show: Boolean = false) {
    val listMap = mutableMapOf<Int, VV>()

    data class VV(val v: Int, val nonzero: List<Int>)

    fun addKProducts() : List<Product> {
        val total = 1 shl k
        repeat(total) { genVVlist(k, it) }

        val result = mutableListOf<Product>()
        repeat(total) { result.add( calcw(it) ) }
        return result
    }

    fun genVVlist(k: Int, ival: Int) {
        var vv = ival
        val vlist = List(k) {
            val v = if (vv and 1 == 1) 1 else 0
            vv = vv shr 1
            v
        }
        val nonzero = nonzero(vlist)
        if (show) println(" ${ival} == ${vlist} == ${nonzero}")
        listMap[ival] = VV(ival, nonzero)
    }

    fun calcw(ival: Int): Product {
        val vv = listMap[ival]!!

        val pair = when (vv.nonzero.size) {
            0 -> Pair( 0, 0)
            1 -> Pair( 0, -(vv.nonzero[0]+1))
            2 -> Pair( -(vv.nonzero[0]+1), -(vv.nonzero[1]+1))
            else -> divide(vv.nonzero)
        }

        if (show) println(" nz=${vv.nonzero.size}: $ival -> $pair")
        return Product(vv.nonzero, pair)
    }

    fun nonzero(vlist: List<Int>): List<Int> {
        val result = mutableListOf<Int>()
        vlist.forEachIndexed { idx, it -> if (it != 0) result.add(idx) }
        return result
    }

    fun binary(nonzero: List<Int>): List<Int> {
        val result = IntArray(k)
        nonzero.forEach { result[it] = 1 }
        return result.toList()
    }

    fun divide(nonzero: List<Int>): Pair<Int, Int> {
        val size2 = nonzero.size / 2
        val left = binary(nonzero.subList(0, size2))
        val right = binary(nonzero.subList(size2, nonzero.size))

        //println(" require nz=${nonzero.size}: ${left} + ${right}) == ${left.addComponents(right)} == ${vlist} ")
        val vlist = binary(nonzero)
        require(left.addComponents(right) == vlist)

        val leftFromMap = listMap[toInt(left)]!!
        val rightFromMap = listMap[toInt(right)]!!

        //println(" require nz=${nonzero.size}: ${leftFromMap.vlist} + ${rightFromMap.vlist}) == ${leftFromMap.vlist.addComponents(rightFromMap.vlist)} == ${vlist} ")
        require(binary(leftFromMap.nonzero).addComponents(binary(rightFromMap.nonzero)) == vlist)

        // singletons must reference the bases directly
        val leftVal = if (leftFromMap.nonzero.size == 1) -(leftFromMap.nonzero[0]+1) else leftFromMap.v
        val rightVal = if (rightFromMap.nonzero.size == 1) -(rightFromMap.nonzero[0]+1) else rightFromMap.v
        return Pair(leftVal, rightVal)
    }

    fun List<Int>.addComponents(other: List<Int>) : List<Int> {
        require (this.size == other.size)
        return List(this.size) { this[it] + other[it]}
    }
}

fun toInt(binary: List<Int>): Int {
    var result = 0
    binary.forEachIndexed { idx, it ->
        if (it != 0) result = result + (1 shl idx)
    }
    return result
}

private const val SpecialOneFactor = Integer.MIN_VALUE

class FEexp(val group: GroupContext, exps: List<ElementModQ>, show: Boolean = false) {
    // there are k exponents which are integers of bitlength t = actualBitLength
    val k = exps.size
    val EAmatrix = BitMatrix(exps.map { it.byteArray() })
    val width = EAmatrix.firstNonZeroColumn()

    // vector addition chain
    val vaChain = VectorAdditionChain(k)

    init {
        showConstruction = show
        if (debug1) println("EAmatrix bitLength=$width, bitCount=${EAmatrix.countBits(width)}")

        val products = mutableMapOf<BitMatrix.ColVector, List<Int>>()
        repeat(width) { colIdx ->
            val vecIdx = width - colIdx - 1 // idx reversed
            val colv = EAmatrix.colVector(vecIdx) // will be length k

            val baseIndexes = mutableListOf<Int>()
            repeat(k) { rowIdx ->
                if (colv.isBitSet(rowIdx)) {
                    baseIndexes.add(rowIdx)
                }
            }
            products[colv] = baseIndexes
        }

        if (showProducts) print(buildString {
            appendLine("products")
            products.forEach { (key, value) ->
                appendLine(" ${key.toInt()} == $value ")
            }
            appendLine()
        })
        vaChain.addProducts(KProducts(k, show).addKProducts())
        if (show) println()

        var result = AdditionChainElem(k) // not actually used because of addFirstFactor()
        repeat(width) { colIdx ->
            val vecIdx = width - colIdx - 1 // start with high order bit, it gets shifted on each square
            val colv = EAmatrix.colVector(vecIdx)
            val factor = products[colv]!!

            if (result.isEmpty()) {
                // first time. wont be empty as long as weve calculated actualBitLength.
                result = vaChain.addFirstFactor(factor)
            } else {
                result = vaChain.addSquare(result)
                if (factor.isNotEmpty()) {
                    result = vaChain.addFactor(result, factor)
                }
            }
        }
        val notmissing = vaChain.countRef()
        val ratio = notmissing.toDouble()/vaChain.chain.size
        println( "  f = ${exps.size} chain size = ${vaChain.chain.size } notmissing = $notmissing ratio = ${ratio.sigfig(2)}")
        if (show) println(vaChain)
    }

    fun prodPowP(bases: List<ElementModP>, show: Boolean = false): ElementModP {
        var countMultiply = 0
        val a = mutableListOf<ElementModP>()

        vaChain.w.forEach { wentry ->
            if (wentry.index == 66)
                println("there")

            if (wentry.i1 == 0 || wentry.refCount == 0) { // skip
                a.add(group.ZERO_MOD_P);
            } else if (wentry.i1 == SpecialOneFactor) { // first element glitch
                val f2 = if (wentry.i2 < 0) bases[-wentry.i2 - 1] else a[wentry.i2]
                a.add(f2)
            } else {
                val f1 = if (wentry.i1 < 0) bases[-wentry.i1 - 1] else a[wentry.i1]
                val f2 = if (wentry.i2 < 0) bases[-wentry.i2 - 1] else a[wentry.i2]
                a.add(f1 * f2)
                countMultiply++
            }
            println("  fff ${a.last().toStringShort()} from $wentry")
        }
        if (show) println("countMultiply=$countMultiply")
        return a.last()
    }
}