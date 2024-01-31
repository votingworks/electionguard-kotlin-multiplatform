package electionguard.exp

import electionguard.core.*
import java.math.BigInteger
import java.util.*
import kotlin.math.max
import kotlin.math.min

// initial ports from vcr
// BigInteger.modPowProd() calls LargeIntegerSimModPowTab or VMG.spowm
// PGroup.expProd() calls BigInteger modPowProd inside of ArrayWorker

// initial port from PGroupSimExpTab. This seems to be fixed bases, variable exps??


// Breakpoint at which exponentiation of * <code>PGroupElement[]</code> is threaded.
private val expThreadThreshold = 100

/**
 * PGroup.expProd() with Array<ElementModP>
 * This might be prodPowP? calc Ap seems to end up in FEexp also.
 * Maybe can use 14.88?
 *
 * Product of all bases to the powers of the given exponents.
 * This uses simultaneous exponentiation and threading.
 *
 * @param bases Bases to be exponentiated.
 * @param integers Powers to be taken.
 * @param bitLength Maximal bit length.
 */
fun expProd(
    group: GroupContext,
    bases: Array<ElementModP>,
    exponents: Array<BigInteger>,
    bitLength: Int
): ElementModP {
    val maxWidth: Int = PGroupSimExpTab.optimalWidth(bitLength)

    // We need to collect partial results from multiple threads in a thread-safe way.
    val parts = mutableListOf<ElementModP>()

    //val worker: ArrayWorker = object : ArrayWorker(bases.size) {

        fun divide(): Boolean {
            return bases.size > expThreadThreshold // nrows > 100 !
        }

        fun work(start: Int, end: Int) {
            var part: ElementModP = group.ONE_MOD_P

            var offset = start

            // Splits parts recieved from ArrayWorker and run through these smaller parts.
            while (offset < end) {
                val width = min(maxWidth.toDouble(), (end - offset).toDouble()).toInt()

                // Compute table for simultaneous exponentiation.
                val tab = PGroupSimExpTab(group, bases, offset, width)

                // Perform simultaneous exponentiation.
                val batch: ElementModP = tab.expProd(exponents, offset, bitLength)

                part = part * batch

                offset += width
            }
            parts.add(part)
        }
    //}

    //worker.work()

    // Multiply the results of the threads.
    var res: ElementModP = group.ONE_MOD_P
    for (part in parts) {
        res = res * part
    }

    return res
}

// initial port from PGroupSimExpTab. This seems to be fixed bases, variable exps??
class PGroupSimExpTab(val  group: GroupContext,
                      val bases: Array<ElementModP>,
                      val offset: Int, //  Position of first basis element to use
                      val width: Int // Number of bases elements to use.
) {
    // Table of pre-computed values. */
    val pre = MutableList(width) { group.ONE_MOD_P } 

    /**
     * Creates a pre-computed table.
     *
     * @param bases Bases used for pre-computation.
     * @param offset Position of first basis element to use.
     * @param width Number of bases elements to use.
     */
    init {

        // Init precalc with bases provided.
        var i = 1
        var j = offset
        while (i < pre.size) {
            pre[i] = bases[j]
            i = i * 2
            j++
        }

        // Perform precalculation using masking for efficiency.
        for (mask in pre.indices) {
            val onemask = mask and (-mask)
            pre[mask] = pre[mask xor onemask] * pre[onemask]
        }
    }

    /**
     * Compute a power-product using the given integer exponents.
     *
     * @param integers Integer exponents.
     * @param offset Position of first exponent to use.
     * @param bitLength Maximal bit length.
     * @return Power product of elements used for pre-computation.
     */
    fun expProd(
        integers: Array<BigInteger>,
        offset: Int,
        bitLength: Int
    ): ElementModP {
        // Loop over bits in integers starting at bitLength - 1.

        var res: ElementModP = group.ONE_MOD_P

        for (i in bitLength - 1 downTo 0) {
            var k = 0

            // Loop over integers to form a word from all the bits at a given position.
            for (j in offset until offset + width) {
                if (integers[j].testBit(i)) {
                    k = k or (1 shl (j - offset))
                }
            }

            // Square.
            res = res * res

            // Multiply.
            res = res * pre[k]
        }
        return res
    }

    companion object {
        /**
         * Theoretically optimal width of pre-computed table.
         * @param bitLength Bit length of exponents used to compute power-products.
         */
        fun optimalWidth(bitLength: Int): Int {
            var width = 1
            var cost = 1.5 * bitLength
            var oldCost: Double
            do {
                oldCost = cost

                width++
                val widthExp = 1 shl width
                cost = ((widthExp + (2 - 1 / widthExp) * bitLength).toDouble()) / width
            } while (width < 31 && cost < oldCost)

            return max(1.0, (width - 1).toDouble()).toInt()
        }
    }
}