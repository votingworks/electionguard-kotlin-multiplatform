@file:OptIn(ExperimentalUnsignedTypes::class)

package electionguard.core

import electionguard.core.Base64.fromSafeBase64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestConstants {
    @Test
    fun saneConstantsBig() {
        val p = Primes4096.largePrimeBytes.toHaclBignumP(mode = ProductionMode.Mode4096)
        val q = Primes4096.smallPrimeBytes.toHaclBignumQ()
        val qInv = b64Production4096P256MinusQ.fromSafeBase64().toHaclBignumQ()
        val g = Primes4096.generatorBytes.toHaclBignumP(mode = ProductionMode.Mode4096)
        val r = Primes4096.residualBytes.toHaclBignumP(mode = ProductionMode.Mode4096)

        val big1_256 = 1U.toHaclBignumQ()
        val big1_4096 = 1U.toHaclBignumP(mode = ProductionMode.Mode4096)

        assertTrue(p.gtP(big1_4096, mode = ProductionMode.Mode4096))
        assertFalse(big1_4096.gtP(p, mode = ProductionMode.Mode4096))
        assertTrue(q gtQ big1_256)
        assertFalse(big1_256 gtQ q)
        assertTrue(g.gtP(big1_4096, mode = ProductionMode.Mode4096))
        assertTrue(r.gtP(big1_4096, mode = ProductionMode.Mode4096))
        assertTrue(qInv gtQ big1_256)
        assertTrue(g.ltP(p, mode = ProductionMode.Mode4096))
    }

    @Test
    fun saneConstantsSmall() {
        val p = b64TestP.fromSafeBase64().toHaclBignumP(mode = ProductionMode.Mode4096)
        val q = b64TestQ.fromSafeBase64().toHaclBignumQ()
        val g = b64TestG.fromSafeBase64().toHaclBignumP(mode = ProductionMode.Mode4096)
        val r = b64TestR.fromSafeBase64().toHaclBignumP(mode = ProductionMode.Mode4096)

        val ip = intTestP.toUInt().toHaclBignumP(mode = ProductionMode.Mode4096)
        val iq = intTestQ.toUInt().toHaclBignumQ()
        val ig = intTestG.toUInt().toHaclBignumP(mode = ProductionMode.Mode4096)
        val ir = intTestR.toUInt().toHaclBignumP(mode = ProductionMode.Mode4096)

        assertContentEquals(ip, p, "P")
        assertContentEquals(iq, q, "Q")
        assertContentEquals(ig, g, "G")
        assertContentEquals(ir, r, "R")
    }

    @Test
    fun testProductionGroups4096() {
        println("testProductionGroups4096")
        val mode = PowRadixOption.LOW_MEMORY_USE
        val productionGroups4096 =
            ProductionGroupContext(
                pBytes = Primes4096.largePrimeBytes,
                qBytes = Primes4096.smallPrimeBytes,
                gBytes = Primes4096.generatorBytes,
                rBytes = Primes4096.residualBytes,
                p256minusQBytes = b64Production4096P256MinusQ.fromSafeBase64(),
                name = "production group, ${mode.description}, 4096 bits",
                powRadixOption = mode,
                productionMode = ProductionMode.Mode4096,
                numPBits = Primes4096.nbits.toUInt(),
                numPBytes = Primes4096.nbits.toUInt() / 8U,
                numPLWords = Primes4096.nbits.toUInt() / 64U,
            )
    }

}