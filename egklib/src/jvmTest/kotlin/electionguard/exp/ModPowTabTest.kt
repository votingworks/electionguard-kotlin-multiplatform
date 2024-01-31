package electionguard.exp

import electionguard.core.ElementModP
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.util.sigfig
import org.junit.jupiter.api.Test
import org.cryptobiotic.bigint.BigInteger
import kotlin.test.assertEquals

class ModPowTabTest {
    val group = productionGroup()
    val modulus = BigInteger(1, group.constants.largePrime)
    val modulusM = java.math.BigInteger(1, group.constants.largePrime)

    @Test
    fun testOptimalWidth() {
        var bitLength = 8
        repeat(12) {
            println("optimalWidth (nexps at a time) for $bitLength = ${ModPowTab.optimalWidth(bitLength)}")
            bitLength *= 2
        }
    }

    @Test
    fun testTableCreation() {
        //     class LargeIntegerSimModPowTab(bases: List<BigInteger>, offset: Int, val width: Int, val modulus: BigInteger) {

        val nrows = 3
        val bases = List(nrows) { group.gPowP(group.randomElementModQ()).toBig() }
        val table = ModPowTabB(bases, 0, nrows, modulus)
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
    fun testModPowProdBshow() {
        runModPowProdB(100, false)
        runModPowProdB(100, true)
    }

    fun runModPowProdB(nrows: Int, show: Boolean = false) {
        println("runModPowProdB with nrows = $nrows")
        val bases = List(nrows) { group.gPowP(group.randomElementModQ()).toBig() }
        val exps = List(nrows) { group.randomElementModQ().toBig() }

        var starting = getSystemTimeInMillis()

        BigInteger.getAndClearOpCounts()
        val newWay = modPowProdB(bases, exps, modulus)
        val timeNew = getSystemTimeInMillis() - starting
        if (show) println(showCountResults(" newWay"))

        // heres the way we do it now
        starting = getSystemTimeInMillis()
        val oldWay = runProdPow(exps, bases, modulus, show)
        val timeOld = getSystemTimeInMillis() - starting

        val ratio = timeOld.toDouble()/timeNew
        println(" nrows=$nrows timeOld = $timeOld, timeNew = $timeNew ratio = ${ratio.sigfig(2)}")
        println(" perrow = ${(timeOld.toDouble()/nrows).sigfig(3)} / ${(timeNew.toDouble()/nrows).sigfig(3)}")
        println("============================================================")

        assertEquals(oldWay, newWay)
    }

    @Test
    fun testModPowProd() {
        val show = false
        runModPowProdQ(100, show)
        runModPowProdQ(200, show)
        runModPowProdQ(400, show)
        runModPowProdQ(800, show)
        runModPowProdQ(100, show)
        println()
    }

    fun runModPowProdQ(nrows: Int, show: Boolean = false) {
        println("runModPowProdQ with nrows = $nrows")
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
        val newWay = modPowProd(basesM, expsM, modulusM)
        val timeNew = getSystemTimeInMillis() - starting
        if (show) println(showCountResults(" newWay"))

        val ratio = timeOld.toDouble()/timeNew
        println(" nrows=$nrows timeOld = $timeOld, timeNew = $timeNew ratio = ${ratio.sigfig(2)}")
        println(" perrow = ${(timeOld.toDouble()/nrows).sigfig(3)} / ${(timeNew.toDouble()/nrows).sigfig(3)}")
        println("============================================================")

        assertEquals(oldWay, newWay)
    }

}
