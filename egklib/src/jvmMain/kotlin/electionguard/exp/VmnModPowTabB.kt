package electionguard.exp

import org.cryptobiotic.bigint.BigInteger
import java.util.*
import kotlin.math.min

/**
 * Creates a pre-computed table.
 *
 * @param bases Bases used for pre-computation.
 * @param offset Position of first basis element to use.
 * @param width Number of bases elements to use.
 * @param modulus Underlying modulus.
 */
class VmnModPowTabB(
    bases: List<BigInteger>,
    offset: Int,
    val width: Int, // Width of table of pre-computed values.
    val modulus: BigInteger,
) {
    val size = 1 shl width // 2 ^ width
    val pre = MutableList<BigInteger>(size) { BigInteger.ONE }
    var count = 0

    // count 2^w modMultiply
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
            pre[mask] = pre[mask xor onemask].multiply(pre[onemask]).mod(modulus) // 2^7 multiply and mod
            count++
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
     * count 2*t modMultiply (square == multiply)
     */
    fun modPowProd(
        exponents: List<BigInteger>,
        offset: Int,
        bitLength: Int
    ): BigInteger {
        // Loop over bits in integers starting at bitLength - 1.
        var res: BigInteger = BigInteger.ONE
        for (i in bitLength - 1 downTo 0) {  // 256 times = t
            var k = 0

            // Loop over integers to form a word from all the bits at a given position.
            for (j in offset until offset + width) {    // width times
                if (exponents[j].testBit(i)) {              // true half the time
                    k = k or (1 shl (j - offset))           // shift only
                }
            }

            // Square.
            res = res.multiply(res).mod(modulus)

            // Multiply.
            res = res.multiply(pre[k]).mod(modulus)

            count += 2
        }
        return res
    }
}

// modPowProd7 using bigint.BigInteger
// total count is N/w * (2^w + 2t) + N/w = N * ((2^w + 2t)/w + 1/w)
// w = 7, t = 256 = N*(128 + 512)/7 = N * 91
fun modPowProd7B(
    bases: List<BigInteger>,
    exponents: List<BigInteger>,
    modulus: BigInteger
): BigInteger {
    val bitLength = 256 // exps always 256 bits
    val maxWidth = 7    // so this is fixed also

    // Enabled pure java code ends here
    val results = mutableListOf<BigInteger>()

    // LOOK VMN threads here with ArrayWorker, we are threading one level up, on each column vector

    var offset = 0
    var end = bases.size
    var count = 0
    var countCalls = 0

    // called N/w times (2^w + 2t) modMultiply
    while (offset < end) {
        val width = min(maxWidth, (end - offset))
        // println("modPowProd $offset, ${offset + width} ")

        // Compute table for simultaneous exponentiation.
        val tab = VmnModPowTabB(bases, offset, width, modulus)

        // Perform simultaneous exponentiation.
        val batch: BigInteger = tab.modPowProd(exponents, offset, bitLength)
        results.add(batch)
        count += tab.count
        countCalls++

        offset += width
    }

    // multiply results from each batch
    val result =  results.reduce { a, b -> (a.multiply(b)).mod(modulus) } // N/w modMultiply
    count += results.size
    println(" $count modMultiply = ${count/bases.size} perN")
    return result
}