package electionguard.exp

import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
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

    @Test
    fun testTableCreation() {
        //     class LargeIntegerSimModPowTab(bases: List<BigInteger>, offset: Int, val width: Int, val modulus: BigInteger) {

        val nrows = 3
        val bases = List(nrows) { group.gPowP(group.randomElementModQ()).toBig() }
        val table = VmnModPowTabB(bases, 0, nrows, modulus)
    }

    /*
Init precalc width=3
 pre[1] = bases[0]
 pre[2] = bases[1]
 pre[4] = bases[2]
Perform precalculation
 pre[0] = pre[0].multiply(pre[0])  000
 pre[1] = pre[0].multiply(pre[1])  001
 pre[2] = pre[0].multiply(pre[2])  010
 pre[3] = pre[2].multiply(pre[1])  011
 pre[4] = pre[0].multiply(pre[4])  100
 pre[5] = pre[4].multiply(pre[1])  101
 pre[6] = pre[4].multiply(pre[2])  110
 pre[7] = pre[6].multiply(pre[1])  111
     */

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

    @Test
    fun testRunModPowProdBshow() {
        runModPowProdB(300, false)
        runModPowProdB(600, false)
        runModPowProdB(900, false)
        runModPowProdB(1200, false)
        println()
    }

    fun runModPowProdB(nrows: Int, show: Boolean = false) {
        val bases = List(nrows) { group.gPowP(group.randomElementModQ()).toBig() }
        val exps = List(nrows) { group.randomElementModQ().toBig() }

        val (newWay, timeNew) = runModPowProdB(bases, exps, show)
        println("runModPowProdB with nrows =$nrows  time = $timeNew msecs, per_row = ${(timeNew.toDouble()/nrows).sigfig(3)} msecs")
    }

    fun runModPowProdB(bases: List<BigInteger>, exps: List<BigInteger>, show: Boolean = false): Pair<BigInteger, Long> {
        var starting = getSystemTimeInMillis()
        BigInteger.getAndClearOpCounts()

        val newWay = modPowProd7B(bases, exps, modulus)

        val timeNew = getSystemTimeInMillis() - starting
        if (show) println(showCountResults(" newWay (modPowProd7B)"))
        return Pair(newWay, timeNew)
    }

    @Test
    fun testModPowProd() {
        val show = false
        compareModPowProdQ(300, show)
        compareModPowProdQ(1000, show)
        compareModPowProdQ(2000, show)
        compareModPowProdQ(3000, show)
        println()
    }

    @Test
    fun testModPowProdshow() {
        compareModPowProdQ(1000, true)
    }

    // compare LargeInteger.modPowProd vs BigIntegerB.modPow
    fun compareModPowProdQ(nrows: Int, show: Boolean = false) {
        println("compareModPowProdQ with nrows = $nrows")
        val bases = List(nrows) { group.gPowP(group.randomElementModQ()) }
        val exps = List(nrows) { group.randomElementModQ() }

        var starting = getSystemTimeInMillis()

        // heres the way we do it now, using java.math
        val org = bases.mapIndexed { idx, it -> it powP exps[idx] }.reduce { a, b -> (a * b) }
        val oldWay = org.toBigM()
        val timeOld = getSystemTimeInMillis() - starting
        if (show) println(showCountResults(" oldWay"))

        val basesM = bases.map { it.toBigM() }
        val expsM = exps.map { it.toBigM() }


        starting = getSystemTimeInMillis()
        BigInteger.getAndClearOpCounts()
        val newWay = modPowProd7(basesM, expsM, modulusM)
        val timeNew = getSystemTimeInMillis() - starting
        if (show) println(showCountResults(" newWay"))

        val ratio = timeOld.toDouble()/timeNew
        println(" nrows=$nrows timeOld = $timeOld, timeNew = $timeNew ratio = ${ratio.sigfig(2)}")
        println(" per_row = ${(timeOld.toDouble()/nrows).sigfig(3)} / ${(timeNew.toDouble()/nrows).sigfig(3)}")
        println("============================================================")

        assertEquals(oldWay, newWay)
    }

}
