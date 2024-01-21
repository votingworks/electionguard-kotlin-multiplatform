package electionguard.core

import org.junit.jupiter.api.Assertions.assertFalse
import kotlin.test.Test
import kotlin.test.assertTrue

class TestSetMembership {
    val group = productionGroup()

    @Test
    fun testSetMembershipZero() {
        assertFalse(checkMembership(group.ZERO_MOD_P))
    }

    @Test
    fun testSetMembershipG() {
        val anyq = group.randomElementModQ()
        val checkit = group.gPowP(anyq)
        assertTrue(checkMembership(checkit))
    }

    @Test
    fun testSetMembershiNotG() {
        val checkit = group.TWO_MOD_P powP group.TWO_MOD_Q
        assertFalse(checkMembership(checkit))
    }

    fun checkMembership(x: ElementModP): Boolean {
        return x.isValidResidue()
    }

}