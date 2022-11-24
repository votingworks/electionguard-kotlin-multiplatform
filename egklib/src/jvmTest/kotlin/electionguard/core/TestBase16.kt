package electionguard.core

import electionguard.core.Base16.fromHex
import electionguard.core.Base16.toHex
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.positiveLong
import io.kotest.property.checkAll
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

class TestBase16 {
    @Test
    fun basicsBase16() {
        val bytes = 1.toBigInteger().toByteArray()
        val b16lib = bytes.toHex()
        assertEquals("01", b16lib)

        val bytesAgain = b16lib.fromHex()
        assertContentEquals(bytes, bytesAgain)
    }

    @Test
    fun badInputFails() {
        assertNull("XYZZY".fromHex())
    }

    @Test
    fun comparingBase16() {
        runTest {
            checkAll(Arb.positiveLong()) { x ->
                val bytes = x.toBigInteger().toByteArray()
                val b16lib = bytes.toHex()
                val bytesAgain = b16lib.fromHex()
                assertContentEquals(bytes, bytesAgain)
            }
        }
    }

    @Test
    fun comparingBase16ToJavaByteArray() {
        runTest {
            checkAll(Arb.byteArray(Arb.int(1, 200), Arb.byte())) { bytes ->
                val b16lib = bytes.toHex()
                val bytesAgain = b16lib.fromHex()
                assertContentEquals(bytes, bytesAgain)
            }
        }
    }
}