@file:OptIn(ExperimentalUnsignedTypes::class)

package electionguard

import kotlin.math.ceil

// Modular exponentiation performance improvements based on a design by Olivier Pereira
// https://github.com/pereira/expo-fixed-basis/blob/main/powradix.py

/** Different acceleration options for the `PowRadix` acceleration of modular exponentiation. */
enum class PowRadixOption {
    NO_ACCELERATION,
    LOW_MEMORY_USE,  // 4.8MB per instance of PowRadix
    HIGH_MEMORY_USE,  // 84MB per instance
    EXTREME_MEMORY_USE,  // 573MB per instance
}

/**
 * The basis is to be used with future calls to the `pow` method, such that
 * `PowRadix(basis, ...).pow(e) == basis powP e, except the computation
 * will run much faster. By specifying which `PowRadixOption` to use, the
 * table will either use more or less memory, corresponding to greater
 * acceleration.
 *
 * `PowRadixOption.NO_ACCELERATION` uses no extra memory and just calls `powmod`.
 *
 * `PowRadixOption.LOW_MEMORY_USE` corresponds to 4.2MB of state per instance of PowRadix.
 *
 * `PowRadixOption.HIGH_MEMORY_USE` corresponds to 84MB of state per instance of PowRadix.
 *
 * `PowRadixOption.EXTREME_MEMORY_USE` corresponds to 537MB of state per instance of PowRadix.
 */
class PowRadix(val basis: ElementModP, acceleration: PowRadixOption) {
    val k: Int
    val tableLength: Int
    val numColumns: Int
    val table: Array<Array<ElementModP>>
    init {
        k = when(acceleration) {
            PowRadixOption.NO_ACCELERATION -> 0
            PowRadixOption.LOW_MEMORY_USE -> 8
            PowRadixOption.HIGH_MEMORY_USE -> 13
            PowRadixOption.EXTREME_MEMORY_USE -> 16
        }

        if (k == 0) {
            tableLength = 0
            numColumns = 0
            table = emptyArray()
        } else {
            tableLength = - (-256 / k)  // double negative takes the ceiling
            var rowBasis = basis
            var runningBasis = rowBasis
            numColumns = 1 shl k
            // row-major table
            table = Array(tableLength) { row ->
                val finalRow = Array(numColumns) { column ->
                    if (column == 0)
                        basis.context.ONE_MOD_P
                    else {
                        val finalColumn = runningBasis
                        runningBasis = runningBasis * rowBasis
                        finalColumn
                    }
                }
                rowBasis = runningBasis
                finalRow
            }
        }
    }

    fun pow(e: ElementModQ): ElementModP {
        if (!basis.context.isCompatible(e.context))
            throw ArithmeticException("incompatible element contexts")

        if (k == 0)
            return basis powP e
        else {
            val slices = e.byteArray().kBitsPerSlice()
            var y = e.context.ONE_MOD_P
            for (i in 0..tableLength) {
                val eSlice = slices[i]
                y = y * table[i][eSlice.toInt()]
            }
            return y
        }
    }

    internal fun ByteArray.kBitsPerSlice(): UShortArray {
        // Input is a "big-endian" byte array, output is "little-endian",
        // since that's the order that the pow method wants to digest
        // these values. We're assuming that k is never more than 16,
        // otherwise we'd need to move up from UShortArray to UIntArray
        // and take a lot more intermediate space for the computation.

        return when (k) {
            16 -> UShortArray(tableLength) {
                // TODO: do the shifting and masking
                this[0].toUShort()
            }
            13 -> UShortArray(tableLength) {
                // TODO: do the shifting and masking
                this[0].toUShort()
            }
            8 -> UShortArray(tableLength) {
                // TODO: do the shifting and masking
                this[tableLength - it].toUShort()
            }
            else -> throw IllegalStateException("k = $k, which isn't supported")
        }
    }
}

//    def pow(self, e: xmpz, normalize_e: bool = True) -> xmpz:
//        """
//        Computes the basis to the given exponent, optionally normalizing
//        the exponent beforehand if it's out of range.
//        """
//        if normalize_e:
//            e = e % self.small_prime
//
//        if self.k == 0:
//            return powmod(self.basis, e, self.large_prime)
//
//        y = xmpz(1)
//        for i in range(self.table_length):
//            e_slice = e[i * self.k : (i + 1) * self.k]
//            y = y * self.table[i][e_slice] % self.large_prime
//        return y