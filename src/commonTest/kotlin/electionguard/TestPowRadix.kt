@file:OptIn(ExperimentalUnsignedTypes::class)

package electionguard

import kotlin.test.Test
import kotlin.test.assertContentEquals

class TestPowRadix {
    @Test
    fun bitSlicingSimplePattern() {
        val testBytes = ByteArray(32) { 0x8F.toByte() }
        val expectedSliceSmall = UShortArray(32) { (0x8F).toUShort() }

        assertContentEquals(expectedSliceSmall, testBytes.kBitsPerSlice(8, 32))

        val expectedSliceExtreme = UShortArray(16) { 0x8F8F.toUShort() }

        assertContentEquals(expectedSliceExtreme, testBytes.kBitsPerSlice(16, 16))

        val expectedSliceLarge =
            UShortArray(22) {
                if (it == 21) {
                    0x8F.toUShort()
                } else if (it % 2 == 0) {
                    0xF8F.toUShort()
                } else {
                    0x8F8.toUShort()
                }
            }

        assertContentEquals(expectedSliceLarge, testBytes.kBitsPerSlice(12, 22))
    }

    @Test
    fun bitSlicingIncreasing() {
        // most significant bits are at testBytes[0], which will start off with value
        // one and then increase on our way through the array

        val testBytes = ByteArray(32) { (it + 1).toByte() }
        val expectedSliceSmall = UShortArray(32) { (32 - it).toUShort() }

        assertContentEquals(expectedSliceSmall, testBytes.kBitsPerSlice(8, 32))

        val expectedSliceExtreme =
            UShortArray(16) {
                val n = 32 - 2 * it - 2 + 1
                ((n shl 8) or (n + 1)).toUShort()
            }

        assertContentEquals(expectedSliceExtreme, testBytes.kBitsPerSlice(16, 16))
    }
}