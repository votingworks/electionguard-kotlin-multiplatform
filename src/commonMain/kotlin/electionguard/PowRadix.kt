@file:OptIn(ExperimentalUnsignedTypes::class)

package electionguard

// Modular exponentiation performance improvements based on a design by Olivier Pereira
// https://github.com/pereira/expo-fixed-basis/blob/main/powradix.py

// DO NOT CHANGE THE CONSTANTS BELOW. For simplicity reasons, and maybe some extra performance,
// ByteArray.kBitsPerSlice is hard-coded around these specific constants.

/** Different acceleration options for the `PowRadix` acceleration of modular exponentiation. */
enum class PowRadixOption(val numBits: Int) {
    NO_ACCELERATION(0),
    LOW_MEMORY_USE(8), // 4.8MB per instance of PowRadix
    HIGH_MEMORY_USE(12), // 42MB per instance
    EXTREME_MEMORY_USE(16), // 573MB per instance
}

/**
 * The basis is to be used with future calls to the `pow` method, such that `PowRadix(basis,
 * ...).pow(e) == basis powP e, except the computation will run much faster. By specifying which
 * `PowRadixOption` to use, the table will either use more or less memory, corresponding to greater
 * acceleration.
 *
 * `PowRadixOption.NO_ACCELERATION` uses no extra memory and just calls `powmod`.
 *
 * `PowRadixOption.LOW_MEMORY_USE` corresponds to 4.2MB of state per instance of PowRadix.
 *
 * `PowRadixOption.HIGH_MEMORY_USE` corresponds to 42MB of state per instance of PowRadix.
 *
 * `PowRadixOption.EXTREME_MEMORY_USE` corresponds to 537MB of state per instance of PowRadix.
 */
class PowRadix(val basis: ElementModP, val acceleration: PowRadixOption) {
    internal val tableLength: Int
    internal val numColumns: Int
    internal val table: Array<Array<ElementModP>>
    init {
        val k = acceleration.numBits

        if (k == 0) {
            tableLength = 0
            numColumns = 0
            table = emptyArray()
        } else {
            tableLength = - (-256 / k) // double negative takes the ceiling
            var rowBasis = basis
            var runningBasis = rowBasis
            numColumns = 1 shl k
            // row-major table
            table =
                Array(tableLength) {
                    /* row -> */
                    val finalRow =
                        Array(numColumns) { column ->
                            if (column == 0) basis.context.ONE_MOD_P else {
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
        basis.context.assertCompatible(e.context)

        //        if (basis == basis.context.G_MOD_P)
        //            println("Computing gPow $e")
        //        else
        //            println("Computing $basis pow $e")

        if (acceleration.numBits == 0) return basis powP e else {
            val slices = e.byteArray().kBitsPerSlice(acceleration, tableLength)
            var y = e.context.ONE_MOD_P
            for (i in 0..(tableLength - 1)) {
                val eSlice = slices[i].toInt() // from UShort to Int so we can do an array lookup
                //                println("- eSlice: $eSlice")
                val nextProd = table[i][eSlice]
                //                println("  - multiplying by $nextProd")
                y = y * nextProd
            }
            //            println("= resulting in $y")
            return y
        }
    }
}

internal fun ByteArray.kBitsPerSlice(
    powRadixOption: PowRadixOption,
    tableLength: Int
): UShortArray {
    // Input is a "big-endian" byte array, output is "little-endian",
    // since that's the order that the pow method wants to digest
    // these values. We're assuming that k is never more than 16,
    // otherwise we'd need to move up from UShortArray to UIntArray
    // and take a lot more intermediate space for the computation.

    // TODO: support values other than the hard-coded 16, 12, and 8-bit slices?

    assert (this.size <= 32 || (this.size == 33 && this[0].toInt() == 0)) {
        "invalid input size (${this.size}), not 32 bytes"
    }

    fun ByteArray.getOrZero(offset: Int) =
        when {
            offset < 0 || offset >= 32 ->
                throw IllegalArgumentException("unexpected offset: $offset")
            size == 32 -> this[offset]
            size == 33 -> this[offset + 1] // skip the leading zero
            offset < (32 - size) -> 0
            else -> this[offset - 32 + size]
        }.toUByte()
            .toInt()

    fun ByteArray.getOrZeroUShort(offset: Int) = getOrZero(offset).toUShort()

    return when (powRadixOption) {
        PowRadixOption.EXTREME_MEMORY_USE ->
            UShortArray(tableLength) {
                assert(tableLength == 16) { "expected tableLength to be 16, got $tableLength" }
                val inputOffset = 32 - 2 * it - 2
                val lowBits = getOrZero(inputOffset + 1)
                val highBits = getOrZero(inputOffset) shl 8
                (highBits or lowBits).toUShort()
            }
        PowRadixOption.HIGH_MEMORY_USE ->
            UShortArray(tableLength) {
                // We've got 10*3 + 2 bytes; each group of three turns into two 12-bit values,
                // leaving the two remaining bytes, which are on the MSB end of the input,
                // corresponding to the last two 12-bit values we output.

                assert(tableLength == 21) { "expected tableLength to be 21, got $tableLength" }
                val inputOffset = 32 - 3 * (it shr 1) - 1
                if (it == 21) {
                    // special case because there are no more high bits
                    (getOrZero(0) shr 4).toUByte().toUShort()
                } else if (it % 2 == 0) {
                    val lowBits = getOrZero(inputOffset)
                    val highBits = (getOrZero(inputOffset - 1) and 0xF) shl 8
                    (highBits or lowBits).toUShort()
                } else {
                    val lowBits = getOrZero(inputOffset - 1) shr 4
                    val highBits = getOrZero(inputOffset - 2) shl 4
                    (highBits or lowBits).toUShort()
                }
            }
        PowRadixOption.LOW_MEMORY_USE ->
            UShortArray(tableLength) {
                assert(tableLength == 32) { "expected tableLength to be 32, got $tableLength" }
                getOrZeroUShort(tableLength - it - 1)
            }
        else ->
            throw IllegalStateException(
                "acceleration k = ${powRadixOption.numBits} bits, which isn't supported"
            )
    }
}
