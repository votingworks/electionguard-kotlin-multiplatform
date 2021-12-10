@file:OptIn(ExperimentalUnsignedTypes::class)

import electionguard.*
import electionguard.Base64.decodeFromBase64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestConstants {
    @Test
    fun saneConstantsBig() {
        val p = b64ProductionP.decodeFromBase64().toHaclBignum4096()
        val q = b64ProductionQ.decodeFromBase64().toHaclBignum256()
        val qInv = b64ProductionP256MinusQ.decodeFromBase64().toHaclBignum256()
        val g = b64ProductionG.decodeFromBase64().toHaclBignum4096()
        val r = b64ProductionR.decodeFromBase64().toHaclBignum4096()

        val big1_256 = 1U.toHaclBignum256()
        val big1_4096 = 1U.toHaclBignum4096()

        assertTrue(p gt4096 big1_4096)
        assertFalse(big1_4096 gt4096 p)
        assertTrue(q gt256 big1_256)
        assertFalse(big1_256 gt256 q)
        assertTrue(g gt4096 big1_4096)
        assertTrue(r gt4096 big1_4096)
        assertTrue(qInv gt256 big1_256)
        assertTrue(g lt4096 p)
    }

    @Test
    fun saneConstantsSmall() {
        val p = b64TestP.decodeFromBase64().toHaclBignum4096()
        val q = b64TestQ.decodeFromBase64().toHaclBignum256()
        val g = b64TestG.decodeFromBase64().toHaclBignum4096()
        val r = b64TestR.decodeFromBase64().toHaclBignum4096()

        val ip = intTestP.toUInt().toHaclBignum4096()
        val iq = intTestQ.toUInt().toHaclBignum256()
        val ig = intTestG.toUInt().toHaclBignum4096()
        val ir = intTestR.toUInt().toHaclBignum4096()

        assertContentEquals(ip, p, "P")
        assertContentEquals(iq, q, "Q")
        assertContentEquals(ig, g, "G")
        assertContentEquals(ir, r, "R")
    }
}