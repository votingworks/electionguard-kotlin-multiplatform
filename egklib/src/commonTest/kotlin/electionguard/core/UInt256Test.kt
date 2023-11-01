package electionguard.core

import electionguard.core.Base16.fromHexSafe
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

    @Test
    fun testElementModQ() {
        runTest {
            val group = productionGroup()
            val s1u = "C49A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206".fromHexSafe()
                .toUInt256safe()
            assertEquals(32, s1u.bytes.size)

            val s1q = s1u.toElementModQ(group)
            assertEquals(64, s1q.base16().length)
        }
    }
}