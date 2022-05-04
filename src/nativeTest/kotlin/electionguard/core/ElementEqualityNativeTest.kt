package electionguard.core

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Shows that native ElementModQ's dont have problem with equals() that Jvm/BigInteger has.
 * They are likely normalized on construction in Hacl.
 */
class ElementEqualityNativeTest {

    @Test
    fun testUInt256() {
        val group = productionGroup() as ProductionGroupContext
        val b = arrayOf(0, -59, -102, -83, 48, 47, 20, -102, 1, -113, -110, 90, -20, 123, -127, -100, 111, -119, 4, 65, -16, -107, 76, 54, -63, -104, -3, 0, 102, -59, -87, 63, 17)
        val ba = ByteArray(b.size, { b[it].toByte()} )
        val biq: ElementModQ = group.binaryToElementModQ(ba) ?: throw IllegalStateException()
        val biu: UInt256 = biq.toUInt256()
        assertEquals("UInt256(0xC59AAD302F149A018F925AEC7B819C6F890441F0954C36C198FD0066C5A93F11)", biu.toString())

        val b2 = arrayOf(-59, -102, -83, 48, 47, 20, -102, 1, -113, -110, 90, -20, 123, -127, -100, 111, -119, 4, 65, -16, -107, 76, 54, -63, -104, -3, 0, 102, -59, -87, 63, 17)
        val ba2 = ByteArray(b2.size, { b2[it].toByte()} )
        val biq2: ElementModQ = group.binaryToElementModQ(ba2) ?: throw IllegalStateException()
        val biu2: UInt256 = biq2.toUInt256()
        assertEquals("UInt256(0xC59AAD302F149A018F925AEC7B819C6F890441F0954C36C198FD0066C5A93F11)", biu2.toString())

        assertEquals(biu2, biu)
        assertEquals(biu2.cryptoHashString(), biu.cryptoHashString())
        assertEquals(biq2.cryptoHashString(), biq.cryptoHashString())
        assertEquals(biq2, biq)
    }
}