package electionguard.core

import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

import electionguard.core.Base64.fromBase64Safe
import electionguard.core.Base64.toBase64

class TestConstants {
    @Test
    fun saneConstantsBig() {
        val p = Primes4096.largePrimeBytes.toBigInteger()
        val q = Primes4096.smallPrimeBytes.toBigInteger()
        val qInv = b64Production4096P256MinusQ.fromBase64Safe().toBigInteger()
        val g = Primes4096.generatorBytes.toBigInteger()
        val r = Primes4096.residualBytes.toBigInteger()

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
        val p = b64TestP.fromBase64Safe().toBigInteger()
        val q = b64TestQ.fromBase64Safe().toBigInteger()
        val g = b64TestG.fromBase64Safe().toBigInteger()
        val r = b64TestR.fromBase64Safe().toBigInteger()

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


    @Test
    fun p256minusQBase64() {
        val p256 = BigInteger.valueOf(1) shl 256
        val q = Primes4096.smallPrimeBytes.toBigInteger()
        val p256minusQ = (p256 - q).toByteArray()
        assertEquals(2, p256minusQ.size)

        val p256minusQBase64 = p256minusQ.toBase64()
        assertEquals(p256minusQBase64, b64Production4096P256MinusQ)

        val p256minusQBytes = b64Production4096P256MinusQ.fromBase64Safe()
        assertEquals(2, p256minusQBytes.size)
    }
}