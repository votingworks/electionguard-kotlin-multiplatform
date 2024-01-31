package electionguard.exp

import java.math.BigInteger
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
 * Creates a pre-computed table.
 *
 * @param bases Bases used for pre-computation.
 * @param offset Position of first basis element to use.
 * @param width Number of bases elements to use.
 * @param modulus Underlying modulus.
 */

//  LargeIntegerSimModPowTab instead of VMG.spowm
//  seems to be fixed bases?
class ModPowTab(bases: List<BigInteger>, offset: Int, val width: Int, val modulus: BigInteger, val show: Boolean = false) {
    val size = 1 shl width // 2 ^ width
    val pre = MutableList<BigInteger>(size) { BigInteger.ONE }

    init {
        if (show) println("Init precalc width=$width")
        var i = 1
        var j = offset
        while (i < pre.size) {
            pre[i] = bases[j]
            if (show) println(" pre[$i] = bases[$j]")
            i *= 2
            j++
        }

        if (show) println("Perform precalculation of all possible combinations, ie 2^width")
        for (mask in pre.indices) {
            val onemask: Int = mask and (-mask)
            pre[mask] = pre.get(mask xor onemask).multiply(pre[onemask]).mod(modulus)
            if (show) println(" pre[$mask] = pre[${mask xor onemask}].multiply(pre[$onemask])")
        }
    }

    fun modPowProd(exponents: List<BigInteger>, offset: Int, bitLength: Int): BigInteger {
        // Loop over bits in exponents starting at bitLength - 1.
        var res = BigInteger.ONE
        for (i in bitLength - 1 downTo 0) {
            var k = 0

            // Loop over integers to form a word from all the bits at a given position.
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

fun modPowProd(
    bases: List<BigInteger>,
    exponents: List<BigInteger>,
    modulus: BigInteger
): BigInteger {
    val bitLength = 256 // exps always 256 bits
    val maxWidth = 7 // so this is fixed also

    // Enabled pure java code ends here
    val results = mutableListOf<BigInteger>()

    var part: BigInteger = BigInteger.ONE

    var offset = 0
    var end = bases.size

    while (offset < end) {
        val width = min(maxWidth, (end - offset))
        // println("modPowProd $offset, ${offset + width} ")

        // Compute table for simultaneous exponentiation.
        val tab = ModPowTab(bases, offset, width, modulus)

        // Perform simultaneous exponentiation.
        val batch: BigInteger = tab.modPowProd(exponents, offset, bitLength)

        part = part.multiply(batch).mod(modulus)

        offset += width
    }
    results.add(part)

    var result: BigInteger = BigInteger.ONE
    for (li in results) {
        result = result.multiply(li).mod(modulus)
    }
    return result
}