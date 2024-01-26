package electionguard.core

// Use 14.88 to generate a vector addition chain
// Then use Algorithm 14.104 in Handbook (menezes et al)
private val debug = false
private val debug1 = false
private val showProducts = false
private var showConstruction = false

class AdditionChain(val k: Int, val elems: IntArray = IntArray(k)) {
    var index: Int = -999

    constructor(k: Int, indices: List<Int>) : this(k) {
        indices.forEach { elems[it] = 1 }
    }

    fun clone() = AdditionChain(k, elems.clone())

    fun isEmpty(): Boolean {
        elems.forEach { if (it != 0) return false }
        return true
    }

    fun square() = AdditionChain(k, IntArray(k) { 2 * elems[it] })

    fun product(v: List<Int>): AdditionChain {
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
        other as AdditionChain
        return elems.contentEquals(other.elems)
    }

    override fun hashCode(): Int {
        return elems.contentHashCode()
    }
}

class VectorAdditionChain(val k: Int, val show: Boolean = false) {
    val chain = mutableListOf<AdditionChain>()
    val w = mutableListOf<Pair<Int, Int>>()
    val elemMap = mutableMapOf<AdditionChain, AdditionChain>()

    fun addProducts(products: List<Product>) {
        products.forEach {
            val ac = AdditionChain(k, it.nonbinary)
            chain.add(ac)
            w.add(it.w)
            // leave out the singletons, they should be found directly in the bases
            if (it.nonbinary.size > 1) elemMap[ac] = ac
            ac.index = chain.size - 1
        }
    }

    fun addSquare(result: AdditionChain): AdditionChain {
        val square = result.square()
        add(square)
        w.add(Pair(result.index, result.index))
        if (showConstruction)  println(" addSquare to get ${showLast()}")
        return square
    }

    fun addFirstFactor(v: List<Int>): AdditionChain {
        // assume we must already have this factor
        val factorIdx = findFactor(v)
        if (factorIdx != null) {
            val factorac = AdditionChain(k, v)
            w.add(Pair(SpecialOneFactor, factorIdx))
            add(factorac)
            if (showConstruction) println(" addFirstFactor to get ${showLast()}")
            return factorac
        } else {
            throw RuntimeException("addFirstFactor")
        }
    }

    fun addFactor(prev: AdditionChain, v: List<Int>): AdditionChain {
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
            w.add(Pair(prev.index, factorIdx))
        } else {
            println("  ${w.size} cant find factor $v")
            val factorac = AdditionChain(k, v)
            val wtf = elemMap[factorac]
            w.add(Pair(prev.index, -999))
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
        val factorac = AdditionChain(k, v)
        return elemMap[factorac]?.index
    }

    fun search(v: List<Int>): Int {
        chain.forEachIndexed { idx, it ->
            if (it.contains(v)) return idx
        }
        return -999 // haha
    }

    private fun add(ac: AdditionChain) {
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
            append("  $idx $it")
            if (idx < w.size) append("  ${w[idx]}")
            appendLine()
        }
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

    fun toInt(binary: List<Int>): Int {
        var result = 0
        binary.forEachIndexed { idx, it ->
            if (it != 0) result = result + (1 shl idx)
        }
        return result
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
            val wtf = products.values
            wtf.forEachIndexed { idx, it ->
                appendLine(" $idx $it")
            }
        })
        vaChain.addProducts(KProducts(k, show).addKProducts())
        if (show) println()

        var result = AdditionChain(k) // not actually used because of addFirstFactor()
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
        if (show) println(vaChain)
        if (show) println( "  chain size = ${ vaChain.chain.size }")
    }

    fun prodPowP(bases: List<ElementModP>, show: Boolean = false): ElementModP {
        var countMultiply = 0
        val a = mutableListOf<ElementModP>()

        vaChain.w.forEach { (i1, i2) ->
            if (i1 == 0) { // skip
                a.add(group.ZERO_MOD_P);
            } else if (i1 == SpecialOneFactor) { // first element glitch
                val f2 = if (i2 < 0) bases[-i2 - 1] else a[i2]
                a.add(f2)
            } else {
                val f1 = if (i1 < 0) bases[-i1 - 1] else a[i1]
                val f2 = if (i2 < 0) bases[-i2 - 1] else a[i2]
                a.add(f1 * f2)
                countMultiply++
            }
        }
        if (show) println("countMultiply=$countMultiply")
        return a[a.size-1]
    }
}