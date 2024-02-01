package electionguard.exp

import java.math.BigInteger

class LargeIntegerFixModPowTab(
    basis: BigInteger,
    bitLength: Int,
    width: Int,
    modulus: BigInteger
) {
    // Table containing the precomputed values.
    // VMGJ  FpowmTab tab
    val tab: VmnModPowTab

    /** Bit length of each slice of an exponent. */
    val sliceSize: Int

    init {
        // Determine the number of bits associated with each bases.
        sliceSize = (bitLength + width - 1) / width

        // Create radix element.
        val b: BigInteger = BigInteger.ONE.shiftLeft(sliceSize)

        // Create generators.
        val bases = mutableListOf<BigInteger>()
        bases.add(basis)
        for (i in 1 until bases.size) {
            bases.add(bases[i - 1].modPow(b, modulus))
        }

        // Invoke the pre-computation of the simultaneous exponentiation code.
        tab = VmnModPowTab(bases, 0, width, modulus)

        // vmgj tab = new FpowmTab(basis.value, modulus.value, width, bitLength);
    }


    // Enabled pure java code begins here.
    /**
     * Cuts an integer into the appropriate number of slices.
     *
     * @param exponent Exponent to be slized.
     * @return Input exponent as slices.
     */
    protected fun slice(exponent: BigInteger): IntArray {
        val res = IntArray(sliceSize)

        for (i in 0 until sliceSize) {
            res[i] = 0

            for (j in tab.width - 1 downTo 0) {
                res[i] = res[i] shl 1

                if (exponent.testBit(j * sliceSize + i)) {
                    res[i] = res[i] or 1
                }
            }
        }

        return res
    }

    /**
     * Compute power using the given integer.
     *
     * @param integer Integer exponent.
     * @return Modular power of this instance to the input.
     */
    fun modPow(integer: BigInteger): BigInteger {
        val sliced = slice(integer)

        var res: BigInteger = BigInteger.ONE
        for (i in sliced.indices.reversed()) {
            // Square.
            res = res.multiply(res).mod(tab.modulus)

            // Multiply.
            res = res.multiply(tab.pre.get(sliced[i])).mod(tab.modulus)
        }
        return res

        // VMGJ tab = new FpowmTab(basis.value, modulus.value, width, bitLength);
    }

    companion object {
        fun optimalWidth(bitLength: Int, size: Int): Int {
            var width = 2
            var cost = 1.5 * bitLength
            var oldCost: Double
            do {
                oldCost = cost

                // Amortized cost for table.
                val t = (((1 shl width) - width + bitLength).toDouble()) / size

                // Cost for multiplication.
                val m = (bitLength.toDouble()) / width

                cost = t + m

                width++
            } while (width <= 16 && cost < oldCost)

            // We reduce the theoretical value by one to account for the overhead.
            return width - 1
        }
    }
}