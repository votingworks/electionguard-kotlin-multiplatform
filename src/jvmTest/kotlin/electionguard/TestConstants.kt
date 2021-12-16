package electionguard

import org.junit.jupiter.api.Test
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import com.soywiz.krypto.encoding.*

class TestConstants {
    @Test
    fun saneConstantsBig() {
        val p = b64ProductionP.fromBase64().toBigInteger()
        val q = b64ProductionQ.fromBase64().toBigInteger()
        val qInv = b64ProductionP256MinusQ.fromBase64().toBigInteger()
        val g = b64ProductionG.fromBase64().toBigInteger()
        val r = b64ProductionR.fromBase64().toBigInteger()

        val big1 = BigInteger.valueOf(1)

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

        assertEquals(BigInteger.valueOf(intTestP.toLong()), p)
        assertEquals(BigInteger.valueOf(intTestQ.toLong()), q)
        assertEquals(BigInteger.valueOf(intTestG.toLong()), g)
        assertEquals(BigInteger.valueOf(intTestR.toLong()), r)
    }

    @Test
    fun expectedSmallConstants() {
//        val p = b64TestP.fromBase64()
//        val q = b64TestQ.fromBase64()
//        val g = b64TestG.fromBase64()
//        val r = b64TestR.fromBase64()

        val pbytes = BigInteger.valueOf(intTestP.toLong()).toByteArray()
        val qbytes = BigInteger.valueOf(intTestQ.toLong()).toByteArray()
        val gbytes = BigInteger.valueOf(intTestG.toLong()).toByteArray()
        val rbytes = BigInteger.valueOf(intTestR.toLong()).toByteArray()

        val pp = pbytes.toBase64()
        val qq = qbytes.toBase64()
        val gg = gbytes.toBase64()
        val rr = rbytes.toBase64()

        assertEquals(b64TestQ, qq)
        assertEquals(b64TestG, gg)
        assertEquals(b64TestR, rr)
        assertEquals(b64TestP, pp)
    }
}