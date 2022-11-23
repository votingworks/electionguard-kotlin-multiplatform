package electionguard.core

import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

import electionguard.core.Base64.fromSafeBase64
import electionguard.core.Base64.toBase64

class TestConstants {
    @Test
    fun saneConstantsBig() {
        val p = b64Production4096P.fromSafeBase64().toBigInteger()
        val q = b64Production4096Q.fromSafeBase64().toBigInteger()
        val qInv = b64Production4096P256MinusQ.fromSafeBase64().toBigInteger()
        val g = b64Production4096G.fromSafeBase64().toBigInteger()
        val r = b64Production4096R.fromSafeBase64().toBigInteger()

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
        val p = b64TestP.fromSafeBase64().toBigInteger()
        val q = b64TestQ.fromSafeBase64().toBigInteger()
        val g = b64TestG.fromSafeBase64().toBigInteger()
        val r = b64TestR.fromSafeBase64().toBigInteger()

        assertEquals(BigInteger.valueOf(intTestP.toLong()), p)
        assertEquals(BigInteger.valueOf(intTestQ.toLong()), q)
        assertEquals(BigInteger.valueOf(intTestG.toLong()), g)
        assertEquals(BigInteger.valueOf(intTestR.toLong()), r)
    }

    @Test
    fun expectedSmallConstants() {
//        val p = b64TestP.fromSafeBase64()
//        val q = b64TestQ.fromSafeBase64()
//        val g = b64TestG.fromSafeBase64()
//        val r = b64TestR.fromSafeBase64()

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