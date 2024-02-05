package electionguard.exp

import java.math.BigInteger
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * Creates a pre-computed table.
 *
 * @param bases Bases used for pre-computation.
 * @param offset Position of first basis element to use.
 * @param width Number of bases elements to use.
 * @param modulus Underlying modulus.
 * from VCR's LargeIntegerSimModPowTab
 */
class VmnModPowTab(
    bases: List<BigInteger>,
    offset: Int,
    val width: Int, // Width of table of pre-computed values.
    val modulus: BigInteger,
) {
    val size = 1 shl width // 2 ^ width
    val pre = MutableList<BigInteger>(size) { BigInteger.ONE }

    init {
        var i = 1
        var j = offset
        while (i < pre.size) {
            pre[i] = bases[j]
            i *= 2
            j++
        }

        // Perform precalculation of all possible combinations, ie 2^width.
        for (mask in pre.indices) {
            val onemask = mask and (-mask)
            pre[mask] = pre[mask xor onemask].multiply(pre[onemask]).mod(modulus)
        }
    }

    /**
     * Compute a power-product using the given integer exponents.
     *
     * @param integers Integer exponents.
     * @param offset Position of first exponent to use.
     * @param bitLength Expected bit length of exponents.
     * @return Power product of the generators used during
     * pre-computation to the given exponents.
     */
    fun modPowProd(
        exponents: List<BigInteger>,
        offset: Int,
        bitLength: Int
    ): BigInteger {
        // Loop over bits in integers starting at bitLength - 1.

        var res: BigInteger = BigInteger.ONE

        for (i in bitLength - 1 downTo 0) {
            var k = 0

            // Loop over integers to form a word from all the bits at
            // a given position.
            for (j in offset until offset + width) {
                if (exponents[j].testBit(i)) {
                    k = k or (1 shl (j - offset))
                }
            }

            // Square.
            res = res.multiply(res).mod(modulus)

            // Multiply.
            res = res.multiply(pre[k]).mod(modulus)
        }
        return res
    }

    companion object {

        fun optimalWidth(bitLength: Int): Int {
            // This computes the theoretical optimum.
            var width = 1
            var cost = 1.5 * bitLength
            var oldCost: Double
            do {
                oldCost = cost

                width++
                val widthExp = 1 shl width
                cost = ((widthExp + (2 - 1 / widthExp) * bitLength).toDouble()) / width
            } while (cost < oldCost)

            return max(1.0, (width - 1).toDouble()).toInt()
        }
    }
}

// modPowProd7 using java.math.BigInteger
fun modPowProd7(
    bases: List<BigInteger>,
    exponents: List<BigInteger>,
    modulus: BigInteger
): BigInteger {
    val bitLength = 256 // exps always 256 bits
    val maxWidth = 7 // so this is fixed also

    // Enabled pure java code ends here
    val results = mutableListOf<BigInteger>()

    // LOOK VMN threads here with ArrayWorker, we are threading one level up, on each column vector

    var offset = 0
    var end = bases.size

    while (offset < end) {
        val width = min(maxWidth, (end - offset))
        // println("modPowProd $offset, ${offset + width} ")

        // Compute table for simultaneous exponentiation.
        // doesnt appear that LargeIntegerFixModPowTab is used
        val tab = VmnModPowTab(bases, offset, width, modulus)

        // Perform simultaneous exponentiation.
        val batch: BigInteger = tab.modPowProd(exponents, offset, bitLength)

        results.add(batch)

        offset += width
    }
    // multiply results from each batch
    return results.reduce { a, b -> (a.multiply(b)).mod(modulus) }
}