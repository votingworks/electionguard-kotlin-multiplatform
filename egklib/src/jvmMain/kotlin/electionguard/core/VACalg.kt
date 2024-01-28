package electionguard.core

// Use 14.88 to generate a vector addition chain
// Then use Algorithm 14.104 in Handbook (menezes et al)
private val showRawCV = false
private val showChain1 = false
private val showChain2 = false
private val showCVints = false
private val showChainResult = false
private val showPowP = false
private var showConstruction = false
private var showTerms = false

////////////////////////////////////////////////////////////////////////////////
// this is for matching the exact factor using the cv value. not about the pairs.

class VACElem(val k: Int, val vac: VAC) {
    var index: Int = -999
    var w: Pair<Int, Int> = Pair(0,0)

    constructor(k: Int, cv: Int) : this(k, VAC(k, cv.toVector(k)))

    fun setW(i1: Int, i2: Int) : VACElem {
        this.w = Pair(i1, i2)
        return this
    }

    fun isEmpty(): Boolean {
        return vac.isEmpty()
    }

    fun square() = VACElem(k, vac.square()).setW(index, index)

    fun product(other: VACElem) = VACElem(k, vac.product(other.vac.elems)).setW(index, other.index)

    override fun toString(): String {
        return " $index: ${vac} $w"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VACElem
        return vac.elems.contentEquals(other.vac.elems)
    }

    override fun hashCode(): Int {
        return vac.elems.contentHashCode()
    }
}

class VAChain(val k: Int, val show: Boolean = false) {
    val chain = mutableListOf<VACElem>()
    val elemMap = mutableMapOf<VACElem, VACElem>() // key is cv

    fun addTerms(terms: List<BitVector>) {
        terms.forEach { term ->
            val ac = makeTermFromSum(term)
            chain.add(ac)
            elemMap[ac] = ac
            ac.index = chain.size - 1
        }
    }

    fun makeTermFromSum(term: BitVector): VACElem {
        val (cv1, cv2) = term.sum2!!
        val idx1 = findIndex(cv1)
        val idx2 = findIndex(cv2)
        return VACElem(k, term.cv).setW(idx1, idx2)
    }

    fun findIndex(cv: ColVector): Int {
        if (cv.countOneBits() == 1) {
            val bitno = cv.toNList()[0]
            return -bitno-1 // its a base
        }
        // otherwise search by col vector
        val ac = VACElem(k, cv)
        return elemMap[ac]!!.index
    }

    fun addSquare(result: VACElem): VACElem {
        val square = result.square()
        add(square)
        if (show && showConstruction)  println(" addSquare to get ${showLast()}")
        return square
    }

    fun addFirstFactor(cv: BitVector): VACElem {
        // do we already have it?
        val elem = VACElem(k, cv.cv)
        val already = elemMap[elem]

        val factor = if (already != null) already else {
            // otherwise we have to make it, must be a 1 bit number, but not added
            val bitno = cv.isOneBit()
            if (bitno != null) {
                VACElem(k, cv.cv).setW(SpecialOneFactor, -bitno - 1)
            } else {
                throw RuntimeException()
            }
        }
        add(factor)
        if (show && showConstruction) println(" addFirstFactor to initialize the result = ${showLast()}")
        return factor
    }

    fun addFactor(prev: VACElem, cv: BitVector): VACElem {
        if (cv.cv == 0) return prev
        //if (prev.index == 22)
        //    println("HEY")

        // if its a one bit, we just make it
        val bitno = cv.isOneBit()
        val result = if (bitno != null) {
            val factor = VACElem(k, cv.cv)
            // tricky: make the result, and tell how to make the result with setW
            VACElem(k, prev.vac.product(factor.vac.elems)).setW(prev.index, -bitno - 1)
        } else {
            // otherwise, we better already have it
            val elem = VACElem(k, cv.cv)
            val already = elemMap[elem]!!
            prev.product(already)
        }

        add(result)
        if (show && showConstruction) println(" addFactor $cv to get ${showLast()}")
        return result
    }

    private fun add(elem: VACElem) {
        chain.add(elem)
        // elemMap[elem] = elem
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
}

private const val SpecialOneFactor = Integer.MIN_VALUE

class VACalg(val group: GroupContext, exps: List<ElementModQ>, val show: Boolean = false) {
    // there are k exponents which are integers of bitlength t = actualBitLength
    val k = exps.size
    val EAmatrix = BitMatrix(exps.map { it.byteArray() })
    val width = EAmatrix.firstNonZeroColumn()

    val vaChain = VAChain(k, show)

    init {
        println("EAmatrix k= $k width=$width, bitsOn=${EAmatrix.countBits(width)}")

        val cvints = mutableListOf<BitVector>()
        repeat(width) { colIdx ->
            val vecIdx = width - colIdx - 1 // start with high order bit, it gets shifted on each square
            val colv = EAmatrix.colVector(vecIdx) // will be length k
            cvints.add(BitVector(k, colv.toInt()))
        }
        if (show && showRawCV) print(buildString {
            appendLine("raw column vectors")
            cvints.forEachIndexed { idx, it ->
                appendLine(" $idx $it")
            }
        })

        val terms = MakeTerms2(k, cvints, show && showTerms).getTerms()
        if (show && showTerms) print(buildString {
            appendLine("terms")
            terms.forEachIndexed { idx, it ->
                appendLine(" $idx $it")
            }
        })
        // TODO maybe we shouldnt modify the raw cvints?
        if (show && showCVints) print(buildString {
            appendLine("cvints")
            cvints.forEachIndexed { idx, it ->
                appendLine(" $idx $it ${it.cv.toVector(k).contentToString()}")
            }
        })

        vaChain.addTerms(terms)
        if (show && showChain1) println(vaChain)

        var result = VACElem(k, 0) // not actually used because of addFirstFactor()
        cvints.forEach { cvint ->
            if (result.isEmpty()) {
                // first time. wont be empty as long as weve calculated actualBitLength.1
                result = vaChain.addFirstFactor(cvint)
            } else {
                result = vaChain.addSquare(result)
                result = vaChain.addFactor(result, cvint)
            }
        }
        //val notmissing = vaChain.countRef()
        //val ratio = notmissing.toDouble()/vaChain.chain.size
        if (show && showChain2) println(vaChain)
        if (show && showChainResult) print(buildString {
            appendLine("chainResults")
            vaChain.chain.forEach {
                appendLine(" $it")
            }
        })
        println( "  k = ${exps.size}, width= $width, chain size = ${vaChain.chain.size}")
    }

    fun prodPowP(bases: List<ElementModP>): ElementModP {
        var countMultiply = 0
        val a = mutableListOf<ElementModP>()

        vaChain.chain.forEach { elem ->
            val (i1, i2) = elem.w
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
        return a.last()
    }
}
