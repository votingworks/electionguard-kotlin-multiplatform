package electionguard.core

import java.math.BigInteger
import java.util.*
import kotlin.math.max

// give up on the port for now

// initial ports from vcr
// BigInteger.modPowProd() calls LargeIntegerSimModPowTab or VMG.spowm
// PGroup.expProd() calls BigInteger modPowProd inside of ArrayWorker

// initial port from PGroupSimExpTab. This seems to be fixed bases, variable exps??

// Simultaneous Multiple exponentiation. Experimental.
// Algorithm 14.88 in Handbook (menezes et al)
//
// BigInteger.modPowProd() calls


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
 * @return Product of all bases to the powers of the given
 * exponents.
 */
/*
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
class LargeIntegerSimModPowTab(bases: List<BigInteger>, offset: Int, val width: Int, modulus: BigInteger) {
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