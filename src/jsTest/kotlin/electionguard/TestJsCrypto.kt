package electionguard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TestJsCrypto {
    @Test
    fun randomValuesAreRandom() {
        val r1 = platformRandomValues(32)
        val r2 = platformRandomValues(32)

        assertEquals(32, r1.size)
        assertEquals(32, r2.size)
        // hack because we don't have assertContentNotEquals()
        assertNotEquals(r1.joinToString(","), r2.joinToString(","))
    }
}