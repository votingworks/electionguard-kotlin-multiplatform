package electionguard.core

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class TestSetMembership {
    val group = productionGroup()

    @Test
    fun testSetMembership0() {
        assertTrue(checkMembership(group.ZERO_MOD_P))
    }

    @Test
    fun testSetMembership1() {
        assertTrue(checkMembership(group.ONE_MOD_P))
    }

    @Test
    fun testSetMembership2() {
        assertTrue(checkMembership(group.TWO_MOD_P))
    }

    fun checkMembership(x: ElementModP): Boolean {
        return x.isValidResidue()
    }

}