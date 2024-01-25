package electionguard.core

// Use 14.88 to generate a vector addition chain
// Then use Algorithm 14.104 in Handbook (menezes et al)
private val debug = false
private val debug1 = true
private val debug2 = false
private val show = true

class AdditionChain(val k: Int, private val elems: IntArray = IntArray(k)) {
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

class VectorAdditionChain(val k: Int) {
    val chain = mutableListOf<AdditionChain>()
    val w = mutableListOf<Pair<Int, Int>>()
    val elemMap = mutableMapOf<Int, AdditionChain>()

    fun addProducts(products: Collection<List<Int>>) {
        // for the moment punt on figuring this out
        products.forEach {
            if (it.size == 2) {
                add(AdditionChain(k, it))
                w.add(Pair(-it[0] - 1, -it[1] - 1))
            }
        }
        products.forEach {
            if (it.size == 3) {
                add(AdditionChain(k, it)) // optimistic
                // we need to find a previous one that has two that are contained. for the moment, exhaustive search is ok
                var use = search(it.subList(0, 2))
                if (use >= 0) {
                    w.add(Pair(use, -it[2] - 1))
                } else {
                    use = search(it.subList(1, 3))
                    if (use >= 0) {
                        w.add(Pair(use, -it[0] - 1))
                    } else {
                        use = search(listOf(it[0], it[3]))
                        if (use >= 0) {
                            w.add(Pair(use, -it[1] - 1))
                        } else {
                            throw RuntimeException("cant find previous product to use")
                        }
                    }
                }
            }
        }
    }

    fun addSquare(result: AdditionChain): AdditionChain {
        val square = result.square()
        add(square)
        w.add(Pair(result.index, result.index))
        return square
    }

    fun addFactor(prev: AdditionChain, v: List<Int>): AdditionChain {
        val result = prev.product(v)

        // do we already have this result?
        val resultPrev = elemMap[result.hashCode()]
        if (resultPrev != null) {
            println("already have this factor $resultPrev")
            return resultPrev
        }

        // assume we must already have this factor
        val factorIdx = findFactor(v)
        if (factorIdx != null) {
            w.add(Pair(prev.index, factorIdx))
        } else {
            println("cant find factor $v")
            w.add(Pair(prev.index, -999))
        }

        add(result)
        return result
    }

    fun findFactor(v: List<Int>): Int? {
        if (v.size == 1) {
            return -v[0]-1 // its a base
        }
        // otherwise search by hashcode
        val factorac = AdditionChain(k, v)
        val key = factorac.hashCode()
        return elemMap[key]?.index
    }

    fun search(v: List<Int>): Int {
        chain.forEachIndexed { idx, it ->
            if (it.contains(v)) return idx
        }
        return -999 // haha
    }

    private fun add(ac: AdditionChain) {
        chain.add(ac)
        val key = ac.hashCode()
        elemMap[key] = ac
        ac.index = chain.size - 1
    }

    fun last(): AdditionChain {
        return chain[chain.size - 1]
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

class FEexp(val group: GroupContext, exps: List<ElementModQ>) {
    // there are k exponents which are integers of bitlength t = actualBitLength
    val k = exps.size
    val EAmatrix = BitMatrix(exps.map { it.byteArray() })
    val width = EAmatrix.firstNonZeroColumn()

    // vector addition chain
    val vaChain = VectorAdditionChain(k)

    init {
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
        vaChain.addProducts(products.values)

        if (show) println(buildString {
            appendLine("products")
            val wtf = products.values
            wtf.forEachIndexed { idx, it ->
                appendLine("$idx $it")
            }
        })

        var result = AdditionChain(k)
        repeat(width) { colIdx ->
            if (!result.isEmpty()) {
                result = vaChain.addSquare(result)
                println(" added square ${vaChain.last()}")
            }
            val vecIdx = width - colIdx - 1 // idx reversed
            val colv = EAmatrix.colVector(vecIdx) // will be length k
            val factor = products[colv]!!
            if (factor.isNotEmpty()) {
                result = vaChain.addFactor(result, factor)
                println(" added factor $factor to get ${vaChain.last()}")
            }
        }
        if (show) println(vaChain)
        println( "chain size = ${ vaChain.chain.size }")
    }

    fun prodPowP(bases: List<ElementModP>, show: Boolean = false): ElementModP {
        var countMultiply = 0
        val a = mutableListOf<ElementModP>()

        vaChain.w.forEach { (i1, i2) ->
            val f1 = if (i1 < 0) bases[-i1-1] else a[i1]
            val f2 = if (i2 < 0) bases[-i2-1] else a[i2]
            a.add( f1 * f2)
            countMultiply++
        }
        if (show) println("countMultiply=$countMultiply")
        return a[a.size-1]
    }
}