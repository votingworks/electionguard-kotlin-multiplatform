package electionguard

import electionguard.Base64.fromBase64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gciatto.kt.math.BigInteger

class TestConstants {
    @Test
    fun saneConstantsBig() {
        val p = b64ProductionP.fromBase64().toBigInteger()
        val q = b64ProductionQ.fromBase64().toBigInteger()
        val qInv = b64ProductionP256MinusQ.fromBase64().toBigInteger()
        val g = b64ProductionG.fromBase64().toBigInteger()
        val r = b64ProductionR.fromBase64().toBigInteger()

        val big1 = BigInteger.of(1)

        assertTrue(p > big1)
        assertTrue(q > big1)
        assertTrue(g > big1)
        assertTrue(r > big1)
        assertTrue(qInv > big1)
        assertTrue(q < p)
        assertTrue(g < p)
    }

    @Test
    fun saneConstantsSmall() {
        val p = b64TestP.fromBase64().toBigInteger()
        val q = b64TestQ.fromBase64().toBigInteger()
        val g = b64TestG.fromBase64().toBigInteger()
        val r = b64TestR.fromBase64().toBigInteger()

        assertEquals(BigInteger.of(intTestP), p)
        assertEquals(BigInteger.of(intTestQ), q)
        assertEquals(BigInteger.of(intTestG), g)
        assertEquals(BigInteger.of(intTestR), r)
    }
}