package electionguard

import electionguard.Base64.decodeFromBase64
import org.gciatto.kt.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestConstants {
    @Test
    fun saneConstantsBig() {
        val p = b64ProductionP.decodeFromBase64().toBigInteger()
        val q = b64ProductionQ.decodeFromBase64().toBigInteger()
        val qInv = b64ProductionP256MinusQ.decodeFromBase64().toBigInteger()
        val g = b64ProductionG.decodeFromBase64().toBigInteger()
        val r = b64ProductionR.decodeFromBase64().toBigInteger()

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
        val p = b64TestP.decodeFromBase64().toBigInteger()
        val q = b64TestQ.decodeFromBase64().toBigInteger()
        val g = b64TestG.decodeFromBase64().toBigInteger()
        val r = b64TestR.decodeFromBase64().toBigInteger()

        assertEquals(BigInteger.of(intTestP), p)
        assertEquals(BigInteger.of(intTestQ), q)
        assertEquals(BigInteger.of(intTestG), g)
        assertEquals(BigInteger.of(intTestR), r)
    }
}