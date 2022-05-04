package electionguard.core

import kotlin.test.Test
import java.math.BigInteger
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
}