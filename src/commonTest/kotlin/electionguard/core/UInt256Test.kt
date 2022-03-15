package electionguard.core

import io.kotest.property.arbitrary.filterNot
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class UInt256Test {
    @Test
    fun xorCorrectness() {
        runTest {
            checkAll(uint256s().filterNot { it.isZero() }, uint256s()) { k, m ->
                assertNotEquals(m, k xor m)
                assertEquals(m, k xor (k xor m))
            }
        }
    }

    @Test
    fun smallConstants() {
        assertTrue(UInt256.ZERO.isZero())
        assertEquals(UInt256.ZERO, 0U.toUInt256())
    }
}