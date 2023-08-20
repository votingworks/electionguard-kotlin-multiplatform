package electionguard.ballot

import electionguard.core.Base16.toHex
import electionguard.core.productionGroup
import io.ktor.utils.io.core.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ElectionConfigTest {

    @Test
    fun parameterBaseHashTest() {
        // HP = H(ver; 0x00, p, q, g) ; spec 2.0.0 p 16, eq 4
        // The symbol ver denotes the version byte array that encodes the used version of this specification.
        // The array has length 32 and contains the UTF-8 encoding of the string “v2.0.0” followed by 0x00-
        // bytes, i.e. ver = 0x76322E302E30 ∥ b(0, 27).

        // HP = H(HV ; 00, p, q, g)   spec 1.9 eq 4
        // The symbol HV denotes the version byte array that encodes the used version of this specification.
        // The array has length 32 and contains the UTF-8 encoding of the string "v2.0" followed by 00-
        // bytes, i.e. HV = 76322E30 ∥ b(0, 28).

        val ver = "v2.0.0".toByteArray()
        println("ver = ${ver.toHex()}")
        assertEquals(6, ver.size)
        val HV = ByteArray(32) { if (it < ver.size) ver[it] else 0 }
        println("HV = ${HV.toHex()}")
        assertEquals("76322E302E300000000000000000000000000000000000000000000000000000", HV.toHex())

        val HP = parameterBaseHash(productionGroup().constants)
        println("HP = ${HP.toHex()}")
        assertEquals("2B3B025E50E09C119CBA7E9448ACD1CABC9447EF39BF06327D81C665CDD86296", HP.toHex())
    }
}