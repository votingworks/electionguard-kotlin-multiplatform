package electionguard.exp

import electionguard.core.ElementModP
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.util.Stopwatch
import electionguard.util.sigfig
import org.junit.jupiter.api.Test
import org.cryptobiotic.bigint.BigInteger
import kotlin.test.assertEquals

class VmnModPowTabTest {
    val group = productionGroup()
    val modulus = BigInteger(1, group.constants.largePrime)
    val modulusM = java.math.BigInteger(1, group.constants.largePrime)

    @Test
    fun testOptimalWidth() {
        var bitLength = 8
        repeat(12) {
            println("optimalWidth (nexps at a time) for $bitLength = ${VmnModPowTab.optimalWidth(bitLength)}")
            bitLength *= 2
        }
    }

    // w = 1 count = 514
    //w = 2 count = 258
    //w = 3 count = 173
    //w = 4 count = 132
    //w = 5 count = 108
    //w = 6 count = 96
    //w = 7 count = 91
    //w = 8 count = 96
    //w = 9 count = 113
    //w = 10 count = 153
    //w = 11 count = 232
    //w = 12 count = 384
    //w = 13 count = 669
    //w = 14 count = 1206
    //w = 15 count = 2218
    @Test
    fun testCountMultiplies() {
        val t= 256
        for (w in 1 .. 15) {
            // (2^w + 2t)/w
            var count = ((1 shl w) + 2 * t) / w
            println("w = $w count = $count")
        }
    }

    @Test
    fun testTableCreation() {
        //     class LargeIntegerSimModPowTab(bases: List<BigInteger>, offset: Int, val width: Int, val modulus: BigInteger) {

        val nrows = 3
        val bases = List(nrows) { group.gPowP(group.randomElementModQ()).toBig() }
        val table = VmnModPowTabB(bases, 0, nrows, modulus)
    }

    /*
    @Test
    fun testModPowProdB() {
        val show = false
        runModPowProdB(100, show)
        runModPowProdB(200, show)
        runModPowProdB(400, show)
        runModPowProdB(800, show)
        runModPowProdB(100, show)
    }

    @Test
    fun testCompareModPowProdBshow() {
        compareModPowProdB(1000, true)
    }

    // compare LargeInteger.modPowProdB vs BigIntegerB.modPow
    fun compareModPowProdB(nrows: Int, show: Boolean = false) {
        println("compareModPowProdB with nrows = $nrows")
        val bases = List(nrows) { group.gPowP(group.randomElementModQ()).toBig() }
        val exps = List(nrows) { group.randomElementModQ().toBig() }

        val (newWay, timeNew) = runModPowProdB(bases, exps, show)

        // heres the way we do it now
        val starting = getSystemTimeInMillis()
        val oldWay = runProdPowB(exps, bases, modulus, show)
        val timeOld = getSystemTimeInMillis() - starting

        val ratio = timeOld.toDouble()/timeNew
        println(" nrows=$nrows timeOld = $timeOld, timeNew = $timeNew msecs, ratio = ${ratio.sigfig(2)}")
        println(" per_row = ${(timeOld.toDouble()/nrows).sigfig(3)} / ${(timeNew.toDouble()/nrows).sigfig(3)} msecs")
        println("============================================================")

        assertEquals(oldWay, newWay)
    }

     */

    @Test
    fun testTimeModPowProd7B() {
        timeModPowProd7B(300, false)
        timeModPowProd7B(600, false)
        timeModPowProd7B(900, false)
        timeModPowProd7B(1200, false)
        println()
    }

    fun timeModPowProd7B(nrows: Int, show: Boolean = false) {
        val bases = List(nrows) { group.gPowP(group.randomElementModQ()).toBig() }
        val exps = List(nrows) { group.randomElementModQ().toBig() }

        val (newWay, timeNew) = countModPowProd7B(bases, exps, show)
        println("runModPowProdB with nrows =$nrows  time = $timeNew msecs, per_row = ${(timeNew.toDouble()/nrows).sigfig(3)} msecs")
    }

    //////////////////////////////////////////////////////////////////////////////////

    @Test
    fun testCountModPowProd7B() {
        countModPowProd7B(7)
        countModPowProd7B(1000)
        println()
    }

    // count ops for modPowProd7B
    // get time for modPowProd7 (using java.math.BigInteger)
    fun countModPowProd7B(nexps: Int) {
        val exps = List(nexps) { group.randomElementModQ() }
        val bases = List(nexps) { group.gPowP(group.randomElementModQ()) }

        val basesM = bases.map { it.toBigM() }
        val expsM = exps.map { it.toBigM() }

        val stopwatch = Stopwatch()
        val modPowProd7 = modPowProd7(basesM, expsM, modulusM)
        val took = stopwatch.stop()
        println("*** modPowProd7 ${Stopwatch.perRow(took, nexps)}")
        val orgb = modPowProd7.toBig()

        val basesB = bases.map { it.toBig() }
        val expsB = exps.map { it.toBig() }
        val (productb, timeb) = countModPowProd7B(basesB, expsB, true)

        assertEquals(orgb, productb)
    }

    fun countModPowProd7B(bases: List<BigInteger>, exps: List<BigInteger>, show: Boolean = false): Pair<BigInteger, Long> {
        val stopwatch = Stopwatch()
        BigInteger.getAndClearOpCounts()
        val newWay = modPowProd7B(bases, exps, modulus)
        val timeNew = stopwatch.stop()
        if (show) println(showCountResultsPerRow(" newWay (modPowProd7B)", bases.size))
        return Pair(newWay, timeNew)
    }

    //////////////////////////////////////////////////////////////////////////////////

    @Test
    fun testTimeModPowProd7() {
        timeModPowProd7(1000)
    }

    // compare times of modPowProd7 vs current modPow (oldWay), both using java.math.BigInteger
    fun timeModPowProd7(nrows: Int) {
        println("compare times of modPowProd7 (new)  vs current modPow (old) with nrows = $nrows")
        val bases = List(nrows) { group.gPowP(group.randomElementModQ()) }
        val exps = List(nrows) { group.randomElementModQ() }

        // current modPow (oldWay)
        val stopwatch = Stopwatch()
        val org = bases.mapIndexed { idx, it -> it powP exps[idx] }.reduce { a, b -> (a * b) }
        val oldWay = org.toBigM()
        val timeOld = stopwatch.stop()

        val basesM = bases.map { it.toBigM() }
        val expsM = exps.map { it.toBigM() }

        stopwatch.start()
        BigInteger.getAndClearOpCounts()
        val newWay = modPowProd7(basesM, expsM, modulusM)
        val timeNew = stopwatch.stop()

        println(" timeModPowProd7 (old/new) = ${Stopwatch.ratioAndPer(timeOld, timeNew, nrows)}")

        assertEquals(oldWay, newWay)
    }

}
