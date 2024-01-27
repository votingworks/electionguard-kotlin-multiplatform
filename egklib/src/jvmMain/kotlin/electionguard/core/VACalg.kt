package electionguard.core

import electionguard.util.sigfig

// Use 14.88 to generate a vector addition chain
// Then use Algorithm 14.104 in Handbook (menezes et al)
private val showChain = true
private val showChainResult = false
private val showPowP = false
private var showConstruction = false
private var showTerms = true

class VACElem(val k: Int, val cv: CVInt) {
    var index: Int = -999

    constructor(k: Int) : this(k, CVInt(0, 0))

    fun clone() = VACElem(k, cv)

    fun square() = VACElem(k, CVInt(0, 2 * cv.cv).setSum(index, index))

    fun product(factor: CVInt, factorIdx: Int) = VACElem(k, CVInt(0, cv.cv + factor.cv).setSum(index, factorIdx))

    override fun toString(): String {
        return " $index: $cv"
    }
}

class ChainResult(val k: Int, val elems: IntArray = IntArray(k)) {

    constructor(k: Int, indices: List<Int>) : this(k) {
        indices.forEach { elems[it] = 1 }
    }

    fun clone() = ChainResult(k, elems.clone())

    fun square() = ChainResult(k, IntArray(k) { 2 * elems[it] })

    fun product(factor: CVInt): ChainResult {
        val nonbinary = factor.cv.toNList()
        val result = clone()
        nonbinary.forEach { result.elems[it]++ }
        return result
    }

    override fun toString(): String {
        return " ${elems.contentToString()}"
    }
}

fun MutableList<ChainResult>.square() {
    val result = this.last().square()
    this.add(result)
}

fun MutableList<ChainResult>.product(factor: CVInt) {
    val result = this.last().product(factor)
    this.add(result)
}

class VAChain(val k: Int, val show: Boolean = false) {
    val chain = mutableListOf<VACElem>()
    val elemMap = mutableMapOf<Int, VACElem>()
    val chainResults = mutableListOf<ChainResult>()

    fun addTerms(terms: List<CVInt>) {
        terms.forEach {
            val ac = VACElem(k, it)
            chain.add(ac)
            // leave out the singletons, they should be found directly in the bases LOOK
            // if (it.cv > 0) elemMap[ac] = ac
            elemMap[it.cv] = ac
            ac.index = chain.size - 1
        }
    }

    fun addSquare(result: VACElem): VACElem {
        val square = result.square()
        add(square)
        if (show && showConstruction)  println(" addSquare to get ${showLast()}")
        chainResults.square()
        return square
    }

    fun addFirstFactor(factor: CVInt): VACElem {
        // assume we must already have this factor
        val factorIdx = findFactor(factor)
        if (factorIdx != null) {
            if (factor.sum == null) factor.setSum(SpecialOneFactor, factorIdx)
            val factorac = VACElem(k, factor)
            add(factorac)
            if (show && showConstruction) println(" addFirstFactor to initialize the result = ${showLast()}")
            chainResults.add(ChainResult(k))
            chainResults.product(factor)
            return factorac
        } else {
            throw RuntimeException("addFirstFactor")
        }
    }

    fun addFactor(prev: VACElem, factor: CVInt): VACElem {
        if (factor.cv == 0) return prev

        // assume we must already have this factor
        val factorIdx = findFactor(factor)
        if (factorIdx == null) {
            println("  ${chain.size} cant find factor $factor")
            throw Exception()
        }

        val result = prev.product(factor, factorIdx)
        chainResults.product(factor)

        // do we already have this result?
        val resultPrev = elemMap[result.cv.cv]
        if (resultPrev != null) {
            println("  ${chain.size} already have this factor $resultPrev")
            return resultPrev
        }

        add(result)
        if (show && showConstruction) println(" addFactor $factor to get ${showLast()}")
        return result
    }

    fun findFactor(factor: CVInt): Int? {
        if (factor.cv.countOneBits() == 1) {
            val bitno = factor.cv.toNList()[0]
            return -bitno-1 // its a base
        }
        // otherwise search by colvector
        return elemMap[factor.cv]?.index
    }

    private fun add(elem: VACElem) {
        chain.add(elem)
        elemMap[elem.cv.cv] = elem
        elem.index = chain.size - 1
    }

    fun showLast(): String {
        return "${chain[chain.size - 1]}"
    }

    override fun toString() = buildString {
        appendLine("VectorAdditionChain")
        chain.forEachIndexed { idx, it ->
            appendLine("  $idx $it")
        }
    }

    /*
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

     */
}

private const val SpecialOneFactor = Integer.MIN_VALUE

class VACalg(val group: GroupContext, exps: List<ElementModQ>, show: Boolean = false) {
    // there are k exponents which are integers of bitlength t = actualBitLength
    val k = exps.size
    val EAmatrix = BitMatrix(exps.map { it.byteArray() })
    val width = EAmatrix.firstNonZeroColumn()

    val vaChain = VAChain(k)

    init {
        println("EAmatrix width=$width, bitsOn=${EAmatrix.countBits(width)}")

        val cvints = mutableListOf<CVInt>()
        repeat(width) { colIdx ->
            val vecIdx = width - colIdx - 1 // start with high order bit, it gets shifted on each square
            val colv = EAmatrix.colVector(vecIdx) // will be length k
            cvints.add(CVInt(colIdx, colv.toInt()))
        }
        if (show && showTerms) print(buildString {
            appendLine("cvints")
            cvints.forEachIndexed { idx, it ->
                appendLine(" $idx $it")
            }
        })

        vaChain.addTerms(MakeTerms(k, cvints, show && showTerms).done)

        var result = VACElem(k) // not actually used because of addFirstFactor()
        cvints.forEach { cvint ->
            if (result.cv.cv == 0) {
                // first time. wont be empty as long as weve calculated actualBitLength.
                result = vaChain.addFirstFactor(cvint)
            } else {
                result = vaChain.addSquare(result)
                result = vaChain.addFactor(result, cvint)
            }
        }
        //val notmissing = vaChain.countRef()
        //val ratio = notmissing.toDouble()/vaChain.chain.size
        if (show && showChain) println(vaChain)
        if (show && showChainResult) print(buildString {
            appendLine("chainResults")
            vaChain.chainResults.forEach {
                appendLine(" $it")
            }
        })
        println( "  k = ${exps.size}, width= $width, chain size = ${vaChain.chain.size}")
    }

    fun prodPowP(bases: List<ElementModP>, show: Boolean = false): ElementModP {
        var countMultiply = 0
        val a = mutableListOf<ElementModP>()

        vaChain.chain.forEach { elem ->
            val (i1, i2) = elem.cv.sum!!
            if (i1 == SpecialOneFactor) { // first element glitch
                val f2 = if (i2 < 0) bases[-i2 - 1] else a[i2]
                a.add(f2)
            } else {
                val f1 = if (i1 < 0) bases[-i1 - 1] else a[i1]
                val f2 = if (i2 < 0) bases[-i2 - 1] else a[i2]
                a.add(f1 * f2)
                countMultiply++
            }
            if (show && showPowP) println("  ppp ${a.last().toStringShort()} from $elem")
        }
        if (show) println("countMultiply=$countMultiply")
        return a.last()
    }
}

class CVInt(val orgIdx: Int, val cv: Int) : Comparable<CVInt> {
    var idxReference: Int = SpecialOneFactor
    var sum: Pair<Int, Int>? = null

    constructor(nlist: List<Int>) : this(0, toIntN(nlist))

    fun done() = (idxReference != SpecialOneFactor) || (sum != null)
    fun setReference(ref: Int): CVInt { idxReference = ref; return this }
    fun setSum(i1: Int, i2: Int): CVInt { sum = Pair(i1, i2); return this }

    override fun compareTo(other: CVInt): Int {
        return this.cv - other.cv
    }

    override fun toString() = buildString {
        append(" CVInt(orgIdx=$orgIdx, cv=$cv, bitsOn=${cv.countOneBits()}")
        if (idxReference != SpecialOneFactor) append(", idxReference=$idxReference")
        if (sum != null) append(", sum=$sum")
        append(")")
    }
}

// to list of nonzeros
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

// there are 256 cvints when using ElementModQ
class MakeTerms(val k: Int, val cvints: List<CVInt>, val show: Boolean = false) {
    val working = mutableMapOf<Int, CVInt>() // key = value
    val done = mutableListOf<CVInt>()

    init {
        removeDuplicates()
        removeEmptyAndBaseValues()

        while (working.size > 0) {
            removeSums()
            removeSingleBits()
            splitLargest()
            println("not done=${working.size}")
        }

        if (show) {
            println("done=${done.size}")
            println(buildString {
                done.forEach { appendLine(it) }
            })
        }
        if (show || (working.size > 0)) {
            println(" working=${working.size}")
            println(buildString {
                working.forEach { appendLine(" ${it.value}") }
            })
        }
        if (working.size > 0) {
            throw RuntimeException("working not empty")
        }
    }

    fun removeDuplicates() {
        cvints.forEach {
            val org = working[it.cv]
            if (org == null) {
                working[it.cv] = it
            }
        }
        if (show) println(" removeDuplicates ${cvints.size - working.size}")
    }

    fun removeEmptyAndBaseValues() {
        val removeThese = mutableListOf<CVInt>()
        working.values.forEach {
            if (it.cv.countOneBits() < 2) { // ignore zeros and ones
                removeThese.add(it)
            } else if (it.cv.countOneBits() == 2) { // can use sum of bases
                val bits = it.cv.toNList()
                done.add(it.setSum(-bits[0]-1, -bits[1]-1))
                removeThese.add(it)
            } else {
                working[it.cv] = it
            }
        }
        removeThese.forEach{working.remove(it.cv)}
        if (show) println(" removeEmptyAndBaseValues ${removeThese.size}")
    }

    fun removeSums() {
        if (working.size < 2) return
        val sorted = working.toSortedMap()
        val last = sorted.values.last().cv
        var count = 0

        for (first in 0 until sorted.size) {
            if (sorted[first]!!.cv + sorted[first + 1]!!.cv > last) break // dont bother continuing
            val firstCV = sorted[first]!!

            for (second in first + 1 until sorted.size) {
                val secondCV = sorted[second]!!
                val test = firstCV.cv + secondCV.cv
                val theSum = sorted[test]
                if (theSum != null && !theSum.done()) {
                    theSum.sum = Pair(firstCV.orgIdx, secondCV.orgIdx)
                    done.add(theSum)
                    working.remove(theSum.cv)
                    count++
                }
                if (test > last) break // dont bother continuing
            }
        }
        if (show) println(" removeSums= $count")
    }

    // look for terms that are sum of  existing and one base
    fun removeSingleBits() {
        if (working.isEmpty()) return
        if (done.isEmpty()) return
        val removeThese = mutableListOf<CVInt>()
        working.values.forEach { trial ->
            for (doneIdx in 0 until done.size) {
                val test = trial.cv - done[doneIdx].cv
                if (test > 0 && test.countOneBits() == 1) {
                    val bitno = test.toNList()[0]
                    trial.sum = Pair(-bitno - 1, doneIdx) // not orgIdx
                    done.add(trial)
                    removeThese.add(trial)
                    if (show)  println(" removeSingleBits $trial succeeded with ${done[doneIdx]} and ${-bitno - 1}")
                    break
                }
            }
        }
        removeThese.forEach{working.remove(it.cv)}
    }

    fun splitLargest() {
        if (working.isEmpty()) return
        val sorted = working.toSortedMap()

        // split the last one, ie with largest value
        val splitt = sorted.values.last()

        val nonzero = splitt.cv.toNList()
        val size2 = nonzero.size / 2
        val left = binary(nonzero.subList(0, size2))
        val right = binary(nonzero.subList(size2, nonzero.size))

        val leftCV = CVInt(left)
        val rightCV = CVInt(right)

        // look may be duplicates - use cv
        working[leftCV.cv] = leftCV
        working[rightCV.cv] = leftCV

        splitt.setSum(leftCV.cv, rightCV.cv)
        working.remove(splitt.cv)
        done.add(splitt)
    }

    fun binary(nonzero: List<Int>): List<Int> {
        val result = IntArray(k)
        nonzero.forEach { result[it] = 1 }
        return result.toList()
    }
}