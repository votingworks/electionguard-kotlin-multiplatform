package electionguard.ballot

import electionguard.core.Base16.toHex
import io.ktor.utils.io.core.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ElectionConfigTest {

    @Test
    fun parameterBaseHashTest() {
        // HP = H(HV ; 00, p, q, g)   spec 1.9 eq 4
        // The symbol HV denotes the version byte array that encodes the used version of this specification.
        // The array has length 32 and contains the UTF-8 encoding of the string "v2.0" followed by 00-
        // bytes, i.e. HV = 76322E30 ∥ b(0, 28).

        val version = "v2.0".toByteArray()
        val HV = ByteArray(32) { if (it < 4) version[it] else 0 }
        assertEquals("76322E3000000000000000000000000000000000000000000000000000000000", HV.toHex())

        val parameterBaseHash = parameterBaseHash()
        assertEquals("AB91D83C3DC3FEB76E57C2783CFE2CA85ADB4BC01FC5123EEAE3124CC3FB6CDE", parameterBaseHash.toHex())
    }
}