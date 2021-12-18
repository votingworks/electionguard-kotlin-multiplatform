@file:OptIn(ExperimentalUnsignedTypes::class)

package electionguard

import io.kotest.property.checkAll
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TestPowRadix {
    @Test
    fun bitSlicingSimplePattern() {
        val testBytes = ByteArray(32) { 0x8F.toByte() }
        val expectedSliceSmall = UShortArray(32) { (0x8F).toUShort() }

        assertContentEquals(
            expectedSliceSmall,
            testBytes.kBitsPerSlice(PowRadixOption.LOW_MEMORY_USE, 32)
        )

        val expectedSliceExtreme = UShortArray(16) { 0x8F8F.toUShort() }

        assertContentEquals(
            expectedSliceExtreme,
            testBytes.kBitsPerSlice(PowRadixOption.EXTREME_MEMORY_USE, 16)
        )

        val expectedSliceLarge =
            UShortArray(22) {
                if (it == 21) {
                    0x8.toUShort()
                } else if (it % 2 == 0) {
                    0xF8F.toUShort()
                } else {
                    0x8F8.toUShort()
                }
            }

        assertContentEquals(
            expectedSliceLarge,
            testBytes.kBitsPerSlice(PowRadixOption.HIGH_MEMORY_USE, 22)
        )
    }

    @Test
    fun bitSlicingIncreasing() {
        // most significant bits are at testBytes[0], which will start off with value
        // one and then increase on our way through the array

        val testBytes = ByteArray(32) { (it + 1).toByte() }
        val expectedSliceSmall = UShortArray(32) { (32 - it).toUShort() }

        assertContentEquals(
            expectedSliceSmall,
            testBytes.kBitsPerSlice(PowRadixOption.LOW_MEMORY_USE, 32)
        )

        val expectedSliceExtreme =
            UShortArray(16) {
                val n = 32 - 2 * it - 2 + 1
                ((n shl 8) or (n + 1)).toUShort()
            }

        assertContentEquals(
            expectedSliceExtreme,
            testBytes.kBitsPerSlice(PowRadixOption.EXTREME_MEMORY_USE, 16)
        )
    }

    @Test
    fun bitSlicingBasics() {
        val ctx = testGroup()
        val option = PowRadixOption.LOW_MEMORY_USE
        val g = ctx.G_MOD_P
        val powRadix = PowRadix(g, option)

        val bytes = 258.toElementModQ(ctx).byteArray()
        // validate it's big-endian
        assertEquals(1, bytes[bytes.size - 2])
        assertEquals(2, bytes[bytes.size - 1])

        val slices = bytes.kBitsPerSlice(option, powRadix.tableLength)
        // validate it's little-endian
        assertEquals(2.toUShort(), slices[0])
        assertEquals(1.toUShort(), slices[1])
        assertEquals(0.toUShort(), slices[2])
    }

    @Test
    fun testExponentiationLowMem() {
        testExponentiationGeneric(PowRadixOption.LOW_MEMORY_USE)
    }

    @Test
    fun testExponentiationHighMem() {
        testExponentiationGeneric(PowRadixOption.HIGH_MEMORY_USE)
    }

    @Ignore // this runs too slowly
    @Test
    fun testExponentiationExtremeMem() {
        println("Testing extreme PowRadix; requires extra memory, slow one-time cost")
        testExponentiationGeneric(PowRadixOption.EXTREME_MEMORY_USE)
    }

    internal fun testExponentiationGeneric(option: PowRadixOption) {
        // We're using the no-acceleration option for these tests, because the other options will
        // internally accelerate exponentiation with g, which means running g powP e won't use
        // the general-purpose modular-exponentiation code, which is what we want to use to
        // compute our "expected" values, for comparison to what PowRadix produces.

        // First we'll try it with the test group, then with the production group

        testGroup()
            .let { ctx ->
                val powRadix = PowRadix(ctx.G_MOD_P, option)

                // sanity check first, then property check
                assertEquals(ctx.ONE_MOD_P, powRadix.pow(0.toElementModQ(ctx)))
                assertEquals(ctx.G_MOD_P, powRadix.pow(1.toElementModQ(ctx)))
                assertEquals(ctx.G_SQUARED_MOD_P, powRadix.pow(2.toElementModQ(ctx)))

                runProperty {
                    checkAll(propTestFastConfig, elementsModQ(ctx)) { e ->
                        assertEquals(ctx.G_MOD_P powP e, powRadix.pow(e))
                    }
                }
            }

        productionGroup(acceleration = PowRadixOption.NO_ACCELERATION)
            .let { ctx ->
                val powRadix = PowRadix(ctx.G_MOD_P, option)

                assertEquals(ctx.ONE_MOD_P, powRadix.pow(0.toElementModQ(ctx)))
                assertEquals(ctx.G_MOD_P, powRadix.pow(1.toElementModQ(ctx)))
                assertEquals(ctx.G_SQUARED_MOD_P, powRadix.pow(2.toElementModQ(ctx)))

                runProperty {
                    // check fewer cases because it's so much slower
                    checkAll(propTestFastConfig, elementsModQ(ctx)) { e ->
                        assertEquals(ctx.G_MOD_P powP e, powRadix.pow(e))
                    }
                }
            }
    }
}