package electionguard.exp

import electionguard.core.*
import electionguard.util.sigfig
import org.cryptobiotic.bigint.BigInteger
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VACalgTest {
    val group = productionGroup()
    val modulus = BigInteger(1, group.constants.largePrime)

    @Test
    fun testVACexample() {
        val e0 = 30.toElementModQ(group)
        val e1 = 10.toElementModQ(group)
        val e2 = 24.toElementModQ(group)
        val exps = listOf(e0, e1, e2)

        val bases = List(3) { group.gPowP(group.randomElementModQ()) }
        val check =  bases.mapIndexed { idx, it -> it powP exps[idx] }.reduce { a, b -> (a * b) }

        // works
        val org = FEexp(group, exps, true).prodPowP(bases)
        assertEquals(check, org)
        println("///////////////////////////////////////////")

        val esb = exps.map { it.toBig() }
        val basesb = bases.map { it.toBig() }

        val vac = VACalg(group, esb, true)
        val result = vac.prodPowP(basesb)

        val productb = runProdPow(esb, basesb, modulus)
        assertEquals(productb, result)
        val productP = ProductionElementModP( java.math.BigInteger(1, productb.toByteArray()), group as ProductionGroupContext)
        assertEquals(org, productP)
        println()
    }

    @Test
    fun testVACexample2() {
        val e0 = 1231130.toElementModQ(group)
        val e1 = 3462110.toElementModQ(group)
        val e2 = 5673241.toElementModQ(group)
        val exps = listOf(e0, e1, e2)

        val bases = List(3) { group.gPowP(group.randomElementModQ()) }
        val check =  bases.mapIndexed { idx, it -> it powP exps[idx] }.reduce { a, b -> (a * b) }

        // works
        val feResult = FEexp(group, exps, true).prodPowP(bases)
        assertEquals(check, feResult)
        println("///////////////////////////////////////////")

        val esb = exps.map { it.toBig() }
        val basesb = bases.map { it.toBig() }

        val vac = VACalg(group, esb, true)
        val result = vac.prodPowP(basesb)

        val productb = runProdPow(esb, basesb, modulus)
        assertEquals(productb, result)
        println()
    }

    @Test
    fun testVACexample3() {
        val exps = listOf(
            1231130.toElementModQ(group),
            3462110.toElementModQ(group),
            5673241.toElementModQ(group),
            2983477.toElementModQ(group),
            6345902.toElementModQ(group),
            329756.toElementModQ(group),
        )

        val bases = List(exps.size) { group.gPowP(group.randomElementModQ()) }
        val check =  bases.mapIndexed { idx, it -> it powP exps[idx] }.reduce { a, b -> (a * b) }

        // works
        //val feResult = FEexp(group, exps, true).prodPowP(bases)
        //assertEquals(check, feResult)
        println("///////////////////////////////////////////")

        val esb = exps.map { it.toBig() }
        val basesb = bases.map { it.toBig() }

        val vac = VACalg(group, esb, true)
        val result = vac.prodPowP(basesb)

        val productb = runProdPow(esb, basesb, modulus)
        assertEquals(productb, result)
        println()
    }

    @Test
    fun testVACexample4() {
        val exps = listOf(
            12313130.toElementModQ(group),
            34623110.toElementModQ(group),
            56733241.toElementModQ(group),
            32983477.toElementModQ(group),
            63435902.toElementModQ(group),
            3297356.toElementModQ(group),
            12331130.toElementModQ(group),
            34362110.toElementModQ(group),
            56733241.toElementModQ(group),
            29834377.toElementModQ(group),
            63345902.toElementModQ(group),
            3239756.toElementModQ(group),
            )

        val bases = List(exps.size) { group.gPowP(group.randomElementModQ()) }
        val check =  bases.mapIndexed { idx, it -> it powP exps[idx] }.reduce { a, b -> (a * b) }

        // works
        //val feResult = FEexp(group, es, true).prodPowP(bases)
        //assertEquals(check, feResult)
        println("///////////////////////////////////////////")

        val esb = exps.map { it.toBig() }
        val basesb = bases.map { it.toBig() }

        val vac = VACalg(group, esb, true)
        val result = vac.prodPowP(basesb)

        val productb = runProdPow(esb, basesb, modulus)
        assertEquals(productb, result)
        println()
    }

    @Test
    fun testVACnrows() {
        runVAC(1, true)
        runVAC(10, true)
        runVAC(20, true)
        runVAC(30, true)
    }

    fun runVAC(nrows : Int, show: Boolean = false) {
        val exps = List(nrows) { group.randomElementModQ() }
        val bases = List(nrows) { group.gPowP( group.randomElementModQ()) }

        val esb = exps.map { it.toBig() }
        val basesb = bases.map { it.toBig() }

        val vac = VACalg(group, esb, true)
        BigInteger.getAndClearOpCounts()
        val result = vac.prodPowP(basesb)
        println(showCountResults(" prodPowP"))

        println("runProdPow")
        val productb = runProdPow(esb, basesb, modulus)
        assertEquals(productb, result)
        println()
    }

    @Test
    fun testVACnrowsTime1() {
        runVACtiming(1, 1)
        runVACtiming(10, 1)
        runVACtiming(20, 1)
        runVACtiming(30, 1)
    }

    @Test
    fun testVACnrowsTime10() {
        runVACtiming(1, 10)
        runVACtiming(10, 10)
        runVACtiming(20, 10)
        runVACtiming(30, 10)
    }

    @Test
    fun testVACbases() {
        val nexps = 30
        runVACtiming(nexps, 10)
        runVACtiming(nexps, 100)
        runVACtiming(nexps, 10000)
    }

    @Test
    fun testVACexps() {
        val nbases = 1000
        //runVACtiming(10, nbases)
        //runVACtiming(20, nbases)
        runVACtiming(30, 100)
    }

    fun runVACtiming(nexps : Int, nbases: Int) {
        println("runVACtiming nexps = $nexps, nbases = $nbases")

        val exps = List(nexps) { group.randomElementModQ() }
        val esb = exps.map { it.toBig() }

        // how long to build?
        var starting11 = getSystemTimeInMillis()
        val vac = VACalg(group, esb)
        val time11 = getSystemTimeInMillis() - starting11

        var starting12 = getSystemTimeInMillis()
        var countFE = 0

        // how long to calculate. apply same VACalg to all the rows
        BigInteger.getAndClearOpCounts()
        repeat (nbases) {
            val bases = List(nexps) { group.gPowP(group.randomElementModQ()) }
            val basesb = bases.map { it.toBig() }
            vac.prodPowP(basesb)
            countFE += bases.size
        }
        val time12 = getSystemTimeInMillis() - starting12
        println(showCountResults(" vac.prodPowP"))

        // heres the way we do it now
        var starting = getSystemTimeInMillis()
        var countP = 0
        repeat (nbases) {
            val bases = List(nexps) { group.gPowP(group.randomElementModQ()) }
            val basesb = bases.map { it.toBig() }
            val productb = runProdPow(esb, basesb, modulus, true)
        }
        val timePowP = getSystemTimeInMillis() - starting

        // println(" countFE = $countFE countP = $countP")
        val ratio = (time12).toDouble()/timePowP
        println(" timeVAC = $time11 + $time12 = ${time11 + time12}, timePowP = $timePowP time12/timePowP = ${ratio.sigfig(2)}")
        println()
    }
}

/*
runFEtiming nrows = 12, nbases = 1
 timeFE = 58 + 466 = 524, timePowP = 69 ratio = 7.5
runFEtiming nrows = 12, nbases = 10
 timeFE = 27 + 695 = 722, timePowP = 553 ratio = 1.3
runFEtiming nrows = 12, nbases = 50
 timeFE = 16 + 3107 = 3123, timePowP = 1883 ratio = 1.6
runFEtiming nrows = 12, nbases = 100
 timeFE = 9 + 5683 = 5692, timePowP = 3849 ratio = 1.4
runFEtiming nrows = 12, nbases = 1000
 timeFE = 12 + 56623 = 56635, timePowP = 37555 ratio = 1.5
 */

/*
runFEtiming nrows = 8, nbases = 100
 timeFE = 25 + 3931 = 3956, timePowP = 2517 ratio = 1.5
runFEtiming nrows = 12, nbases = 100
 timeFE = 31 + 5728 = 5759, timePowP = 3736 ratio = 1.5
runFEtiming nrows = 16, nbases = 100
 timeFE = 190 + 19006 = 19196, timePowP = 5075 ratio = 3.7
 */

/*
runVACtiming nexps = 30, nbases = 10
EAmatrix k= 30 width=256, bitsOn=3882
  k = 30, width= 256, chain size = 2086
 timeVAC = 4504 + 1484 = 5988, timePowP = 1069 ratio = 5.6
runVACtiming nexps = 30, nbases = 100
EAmatrix k= 30 width=256, bitsOn=3859
  k = 30, width= 256, chain size = 2100
 timeVAC = 4323 + 11282 = 15605, timePowP = 10245 ratio = 1.5
runVACtiming nexps = 30, nbases = 1000
EAmatrix k= 30 width=256, bitsOn=3863
  k = 30, width= 256, chain size = 2073
 timeVAC = 4591 + 108266 = 112857, timePowP = 97069 ratio = 1.1
 runVACtiming nexps = 30, nbases = 10000
   k = 30, width= 256, chain size = 2084
 timeVAC = 4698 + 1068567 = 1073265, timePowP = 958004 ratio = 1.1
 */