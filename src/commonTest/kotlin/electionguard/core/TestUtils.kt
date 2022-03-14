package electionguard.core

import io.kotest.property.Arb
import io.kotest.property.arbitrary.uInt
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TestUtils {
    @Test
    fun integersToByteArrays() {
        assertContentEquals(byteArrayOf(0x12), 0x12U.toULong().toByteArray())
        assertContentEquals(byteArrayOf(0x12, 0x34), 0x1234U.toULong().toByteArray())
        assertContentEquals(
            byteArrayOf(0x12, 0x34, 0x56, 0x78),
            0x12345678U.toULong().toByteArray()
        )
        assertContentEquals(byteArrayOf(0x00, 0x34, 0x56, 0x78), 0x345678U.toULong().toByteArray())
        assertContentEquals(
            byteArrayOf(
                0x12,
                0x34,
                0x56,
                0x78,
                0x9a.toByte(),
                0xbc.toByte(),
                0xde.toByte(),
                0xf0.toByte()
            ),
            0x123456789abcdef0U.toByteArray()
        )
        assertContentEquals(
            byteArrayOf(
                0x00,
                0x00,
                0x56,
                0x78,
                0x9a.toByte(),
                0xbc.toByte(),
                0xde.toByte(),
                0xf0.toByte()
            ),
            0x56789abcdef0U.toByteArray()
        )

        assertContentEquals(byteArrayOf(0x12, 0x34, 0x56, 0x78), 0x12345678U.toByteArray())
        assertContentEquals(byteArrayOf(0x12, 0x34, 0x56, 0x78), 0x12345678.toByteArray())
        assertContentEquals((-0x12345678).toUInt().toByteArray(), (-0x12345678).toByteArray())
        assertContentEquals(byteArrayOf(0x00, 0x34, 0x56, 0x78), 0x345678.toByteArray())
    }

    @Test
    fun testListNullExclusion() {
        assertEquals(listOf(1, 2, 3), listOf(1, 2, 3).noNullValuesOrNull())
        assertEquals(null, listOf(1, 2, 3, null).noNullValuesOrNull())
    }

    @Test
    fun testMapNullExclusion() {
        assertEquals(
            mapOf(1 to "one", 2 to "two"),
            mapOf(1 to "one", 2 to "two").noNullValuesOrNull()
        )
        assertEquals(null, mapOf(1 to "one", 2 to "two", 3 to null).noNullValuesOrNull())
    }

    @Test
    fun byteConcatenation() {
        assertContentEquals(byteArrayOf(1, 2, 3, 4), byteArrayOf(1, 2) + byteArrayOf(3, 4))
        assertContentEquals(
            byteArrayOf(1, 2, 3, 4, 5, 6),
            concatByteArrays(byteArrayOf(1, 2), byteArrayOf(3), byteArrayOf(), byteArrayOf(4, 5, 6))
        )
    }

    @Test
    fun byteArrayXor() {
        runTest {
            checkAll(Arb.uInt(min = 1U), Arb.uInt()) { i, m ->
                val iBytes = i.toByteArray()
                val mBytes = m.toByteArray()

                assertContentNotEquals(mBytes, mBytes xor iBytes)
                assertContentEquals(mBytes, (mBytes xor iBytes) xor iBytes)
            }
        }
    }
}