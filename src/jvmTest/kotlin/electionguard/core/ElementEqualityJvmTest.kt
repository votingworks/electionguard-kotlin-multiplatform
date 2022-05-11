package electionguard.core

import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Shows that ElementModQ's can be non-equal, even when cryptoHashString() and UInt256 are.
 * So where should Elements be normalized? on construction, or on equals?
 */
class ElementEqualityJvmTest {

    @Test
    fun testUInt256() {
        val group = productionGroup() as ProductionGroupContext
        val bi = BigInteger("89378920920032937196531702992192972263302712977973574040976517358784464109329")
        val biq: ElementModQ = ProductionElementModQ(bi, group)
        val biu: UInt256 = biq.toUInt256()
        assertEquals(biu.toString(), "UInt256(0xC59AAD302F149A018F925AEC7B819C6F890441F0954C36C198FD0066C5A93F11)")

        val bi2 = BigInteger("-26413168317283258227039282016494935589967271687666989998481066649128665530607")
        val biq2: ElementModQ = ProductionElementModQ(bi2, group)
        val biu2: UInt256 = biq2.toUInt256()
        assertEquals(biu2.toString(), "UInt256(0xC59AAD302F149A018F925AEC7B819C6F890441F0954C36C198FD0066C5A93F11)")

        assertEquals(biu2, biu)
        assertEquals(biq2.cryptoHashString(), biq.cryptoHashString())

        // ElementModQ not equal because it just compares the underlying BigInteger
        assertNotEquals(biq2, biq)
        assertNotEquals(bi2, bi)
    }

    @Test
    fun testNormalizedBigIntegers() {
        val group = productionGroup() as ProductionGroupContext
        val q2 = group.TWO_MOD_Q
        val q2b: BigInteger = q2.element
        assertNotEquals(q2b.toByteArray().size, 32)

        val ba : ByteArray = q2.element.toByteArray()
        val ban = ba.normalize(32)
        assertEquals(ban.size, 32)
        val q2n = ProductionElementModQ(BigInteger(ban), group)

        assertEquals(q2, q2n)
        val q2nb: BigInteger = q2n.element
        assertNotEquals(q2nb.toByteArray().size, 32)

        assertEquals(q2.element.toString(), q2n.element.toString())
        assertEquals(q2.element, q2n.element)
    }
}