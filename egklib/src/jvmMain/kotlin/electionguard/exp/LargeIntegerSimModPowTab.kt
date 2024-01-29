package electionguard.exp

import java.math.BigInteger
import java.util.*
import java.util.concurrent.Callable
import kotlin.math.max
import kotlin.math.min

// from vcr LargeInteger.modPowProd
// BigInteger.modPowProd() calls LargeIntegerSimModPowTab or VMG.spowm
// PGroup.expProd() calls BigInteger modPowProd inside of ArrayWorker
//
// divide the work by perCore = bases.size / availableCores()
// maybe the BigIntegers may be concatenated?

// In FEexp, we want to precompute the VAC for the exponents, then we can apply that VAC to each of the bases.
// The exponents are spread out into a Matrix nrows x 256 (bit size of ElementModQ). To compute the VAC efficiently,
// we may need to break up the exponent rows. Which is ok to do since we are just forming the products. So one can divide
// by nrows and then multiply products together at the end. So one just computes the VAC for a chunk of the exp rows,
// but then applies that on all the bases (keeping results separate for each column of w or w')
//
// But I dont see where spowm() would store the VOC, so its possible that spowm() is some other algorithm. TBD

/**
 * PGroup.expProd() with PGroupElementArray
 * This is prodColumnPow.
 * maybe can use 14.104
 *
 * calls BigInteger.modPowProd() calls LargeIntegerSimModPowTab or VMG.spowm inside ArrayWorker()
 *
 * Returns the product of all elements in `bases` to the respective powers in `exponents` in an element-wise fashion,
 * i.e., the integer exponents are applied to each "row" of elements gathered from the arrays.
 * This uses simultaneous exponentiation and threading.
 *
 * @param bases Bases to be exponentiated.
 * @param integers Powers to be taken.
 * @param bitLength Maximal bit length.
 * @return Product of all bases to the powers of the given exponents.
 */

// GMP version of LargeInteger.modPowProd
fun modPowProdGmp(bases: Array<BigInteger>, exponents: Array<BigInteger>, modulus: BigInteger): BigInteger? {
    // Removed pure java code here.

    val results: MutableList<BigInteger> =
        Collections.synchronizedList(LinkedList<BigInteger>())

    val worker: ArrayWorker = object : ArrayWorker(bases.size) {

            override fun work(start: Int, end: Int) {
                // Removed pure java code here.
                // Enabled calls to native code begins here.

                val biBases = arrayOfNulls<BigInteger>(end - start)
                val biExponents = arrayOfNulls<BigInteger>(end - start)

                // why is this in a run block?
                run {
                    var i = start
                    var j = 0
                    while (i < end) {
                        biBases[j] = bases[i]
                        i++
                        j++
                    }
                }
                var i = start
                var j = 0
                while (i < end) {
                    biExponents[j] = exponents[i]
                    i++
                    j++
                }

                results.add( VMGspowm(biBases, biExponents, modulus) )
                // Enabled calls to native code ends here
            }
        }
    worker.work()

    var result: BigInteger = BigInteger.ONE
    for (li in results) {
        result = result.multiply(li).mod(modulus)
    }
    return result
}

fun VMGspowm(bases: Array<BigInteger?>, exponents: Array<BigInteger?>, modulus: BigInteger?): BigInteger {
    return BigInteger.ONE
}

// Pure Java version of LargeInteger.modPowProd
/**
 * Each basis in the array of bases is taken to the modular power
 * of the corresponding exponent modulo the modulus and the
 * modular product of the resulting integers is returned.
 *
 * @param bases Array of basis integers.
 * @param exponents Array of exponents.
 * @param modulus Modulus.
 * @return Modular power product of the input arrays.
 */
fun modPowProdJava(
    bases: Array<BigInteger>,
    exponents: Array<BigInteger>,
    modulus: BigInteger
): BigInteger {
    // Enabled pure java code begins here.

    // Compute the maximal bit length of the exponents.

    var tmpBitLength = 0

    for (i in exponents.indices) {
        tmpBitLength = max(exponents[i].bitLength().toDouble(), tmpBitLength.toDouble()).toInt()
    }

    val bitLength = tmpBitLength
    val maxWidth = LargeIntegerSimModPowTab.optimalWidth(bitLength)

    // Enabled pure java code ends here
    val results: MutableList<BigInteger> =
        Collections.synchronizedList(LinkedList<BigInteger>())

    val worker: ArrayWorker =
        object : ArrayWorker(bases.size) {
            override fun work(start: Int, end: Int) {
                // Enabled pure java code begins here.

                var part: BigInteger = BigInteger.ONE

                var offset = start

                // Splits parts received from ArrayWorker and run through
                // these smaller parts.
                while (offset < end) {
                    val width = min(maxWidth.toDouble(), (end - offset).toDouble()).toInt()

                    // Compute table for simultaneous
                    // exponentiation.
                    val tab =
                        LargeIntegerSimModPowTab(
                            bases, offset, width,
                            modulus
                        )

                    // Perform simultaneous exponentiation.
                    val batch: BigInteger =
                        tab.modPowProd(exponents, offset, bitLength)

                    part = part.multiply(batch).mod(modulus)

                    offset += width
                }
                results.add(part)

                // Enabled pure java code ends here
                // Removed native code here.
            }
        }
    worker.work()

    var result: BigInteger = BigInteger.ONE
    for (li in results) {
        result = result.multiply(li).mod(modulus)
    }
    return result
}

/* ??
fun expProd(
    group : GroupContext,
    bases: Array<List<ElementModP>>, // this might be the W or W' ?
    integers: Array<BigInteger>,
    bitLength: Int
): List<ElementModP> {

    var res: List<ElementModP> = MutableList(bases[0].size) {group.ONE_MOD_P }

    for (i in integers.indices) {
        var tmp1: ElementModP
        var tmp2: ElementModP

        // We exploit that the integers may have small absolute
        // value, but we need to take care of negative integers.
        if (integers[i].compareTo(BigInteger.ZERO) > 0) {
            tmp1 = bases[i].exp( pField.toElement(integers[i]) ) // this eventually calls
        } else {
            tmp2 = bases[i].inv()
            tmp1 = tmp2.exp(pField.toElement(integers[i].negate()))
            tmp2.free()
        }

        tmp2 = res
        res = res.mul(tmp1)
    }

    return res
}

 */

//  LargeIntegerSimModPowTab instead of VMG.spowm
class LargeIntegerSimModPowTab(bases: Array<BigInteger>, offset: Int, val width: Int, modulus: BigInteger) {
    val pre = MutableList<BigInteger>(bases.size) { BigInteger.ONE }
    val modulus: BigInteger = modulus

    init {
        var mask = 1

        var onemask: Int
        onemask = offset
        while (mask < pre.size) {
            pre[mask] = bases[onemask]
            mask *= 2
            ++onemask
        }

        mask = 0
        while (mask < pre.size) {
            onemask = mask and -mask
            // pre[mask] = pre[mask xor onemask].mul(pre[onemask]).mod(modulus)
            ++mask
        }
    }

    // same as SMexp
    fun modPowProd(integers: Array<BigInteger>, offset: Int, bitLength: Int): BigInteger {
        var res: BigInteger = BigInteger.ONE

        for (i in bitLength - 1 downTo 0) {
            var k = 0

            for (j in offset until offset + this.width) {
                if (integers[j].testBit(i)) {
                    k = k or (1 shl j - offset)
                }
            }

            res = (res * res).mod(this.modulus)
            res = (res * pre[k]).mod(this.modulus)
        }

        return res
    }

    // same as SMexp
    companion object {
        fun optimalWidth(bitLength: Int): Int {
            var width = 1
            var cost = 1.5 * bitLength.toDouble()

            var oldCost: Double
            do {
                oldCost = cost
                ++width
                val widthExp = 1 shl width
                cost = (widthExp + (2 - 1 / widthExp) * bitLength).toDouble() / width.toDouble()
            } while (cost < oldCost)

            return max(1.0, (width - 1).toDouble()).toInt()
        }
    }
}

abstract class ArrayWorker
/**
 * Creates an instance with the given size.
 *
 * @param size Size of instance.
 */(
    /**
     * Size of input arrays.
     */
    protected var size: Int
) {
    /**
     * Determines if the operation is threaded or not. This should be
     * replaced by subclasses that determines to thread or not based
     * on the cost of an operation.
     *
     * @return True or false depending on if an operation should be
     * threaded or not.
     */
    fun divide(): Boolean {
        return true
    }

    /**
     * Performs the work delegated to a given core.
     *
     * @param start Starting index for the work delegated to the
     * active core.
     * @param end Ending index for the work delegated to the active
     * core.
     */
    abstract fun work(start: Int, end: Int)

    /**
     * Performs the work of this instance.
     */
    fun work() {
        divideWork(this, size)
    }

    companion object {
        /**
         * Determines the number of cores on the machine and divides the
         * work encapsulated in `worker` on this number of
         * threads.
         *
         * @param worker Encapsulation of work to be done.
         * @param size Length of arrays.
         */
        private fun divideWork(worker: ArrayWorker, size: Int) {
            if (worker.divide()) {
                // Fetch a pool of threads.

                // val executor: ThreadPoolExecutor = ThreadPoolExecutor.()

                // Determine how many cores to use and divide the work.
                val usedCores = 42 // Math.min(executor.getCores(), size)
                val perCore = size / usedCores

                // Create a list of callable to work independently.
                val callables: MutableList<ArrayWorkerCallable> =
                    ArrayList(usedCores)
                for (l in 0 until usedCores) {
                    val start = l * perCore
                    var end = (l + 1) * perCore
                    if (l == usedCores - 1) {
                        end = size
                    }
                    callables.add(ArrayWorkerCallable(worker, start, end))
                }

                // Run the threads until they complete and return the
                // results as a list.
                try {
                    ; // executor.invoke(callables)
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw RuntimeException("Interrupted threaded computatation!", ie)
                }
            } else {
                worker.work(0, size)
            }
        }
    }
}

/**
 * Callable processing a segment of arrays.
 *
 * @author Douglas Wikstrom
 */
internal class ArrayWorkerCallable
/**
 * Creates a callable that will perform the given number of
 * operations.
 *
 * @param worker Underlying worker.
 * @param start First index of an element to process (inclusive).
 * @param end Last index of an element to process (exclusive).
 */(
    /**
     * Underlying worker.
     */
    private val worker: ArrayWorker,
    /**
     * Start index for processing.
     */
    private val start: Int,
    /**
     * End index for processing.
     */
    private val end: Int
) : Callable<Any?> {
    override fun call(): Any? {
        worker.work(start, end)
        return null
    }
}