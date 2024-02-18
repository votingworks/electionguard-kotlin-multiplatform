package electionguard.core

import org.junit.jupiter.api.Assertions.assertFalse
import kotlin.test.Test
import kotlin.test.assertTrue

class TestSetMembership {
    val group = productionGroup()

    @Test
    fun testSetMembershipZero() {
        val zero = group.binaryToElementModP(ByteArray(11))!!
        assertFalse(checkMembership(zero))
    }

    @Test
    fun testSetMembershipOne() {
        assertTrue(checkMembership(group.ONE_MOD_P))
    }

    @Test
    fun testSetMembershipG() {
        val anyq = group.randomElementModQ()
        val checkit = group.gPowP(anyq)
        assertTrue(checkMembership(checkit))
    }

    @Test
    fun testSetMembershiNotG() {
        val two = group.binaryToElementModP(ByteArray(1) { 2 })!!
        val checkit = two powP group.TWO_MOD_Q
        assertFalse(checkMembership(checkit))
    }

    fun checkMembership(x: ElementModP): Boolean {
        return x.isValidResidue()
    }

}