package electionguard.core

// Use 14.88 to generate a vector addition chain
// Then use Algorithm 14.104 in Handbook (menezes et al)
private val debug = false
private val debug1 = false
private val showProducts = false
private var showConstruction = false

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
    val elemMap = mutableMapOf<AdditionChain, AdditionChain>()

    /*
    fun addProducts(products: Collection<List<Int>>) {
        // for the moment punt on figuring this out
        products.forEach {
            if (it.size == 2) {
                add(AdditionChain(k, it))
                w.add(Pair(-it[0] - 1, -it[1] - 1))
            }
        }
        products.forEach { listi ->
            if (listi.size > 2) {
                val ac = AdditionChain(k, listi)
                // do we already have it ?
                if (elemMap[ac] != null) {
                    println("already have product $ac")
                } else {
                    // see if we can find a subset that has n - 1 elements
                    repeat(listi.size) { idx ->
                        println("size ${listi.size} $idx")
                        val subset = arrayListOf(listi.size) { listi[it] } .removeAt(idx)
                        val subsetac = AdditionChain(k, subset)
                        val subsetPrev = elemMap[subsetac]
                        if (subsetPrev != null) {
                            add(AdditionChain(k, it))
                            w.add(Pair(subsetPrev.index, -it[idx]-1))
                            println("found subset $ac")
                            return@repeat
                        }
                    }
                }
            }
        }
        println("  added ${w.size} from products")
    } */

    fun alreadyHave(v: List<Int>): Boolean {
        // do we already have this?
        val productac = AdditionChain(k, v)
        return elemMap[productac] != null
    }

    // this apparently can deal with k=3
    fun addProducts(products: Collection<List<Int>>) {

        // for the moment punt on figuring this out
        products.forEach {
            if (it.size == 2 && !alreadyHave(it)) {
                add(AdditionChain(k, it))
                w.add(Pair(-it[0] - 1, -it[1] - 1))
                if (showConstruction)  println(" addProduct size2 ${showLast()}")
            }
        }
        products.forEach {
            if (it.size == 3 && !alreadyHave(it)) {
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
                if (showConstruction) println(" addProduct size3 ${showLast()}")
            }
        }
        if (showConstruction) println("added ${w.size} from products")
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
        vaChain.addProducts(products.values)
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
        if (show) println( "chain size = ${ vaChain.chain.size }")
    }

    fun prodPowP(bases: List<ElementModP>, show: Boolean = false): ElementModP {
        var countMultiply = 0
        val a = mutableListOf<ElementModP>()

        vaChain.w.forEach { (i1, i2) ->
            if (i1 == SpecialOneFactor) { // first element glitch
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