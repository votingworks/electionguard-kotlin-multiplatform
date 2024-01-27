package electionguard.core

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VACalgTest {
    val group = productionGroup()

    @Test
    fun testVACexample() {
        val e0 = 30.toElementModQ(group)
        val e1 = 10.toElementModQ(group)
        val e2 = 24.toElementModQ(group)
        val es = listOf(e0, e1, e2)

        val bases = List(3) { group.gPowP(group.randomElementModQ()) }

        // works
        //FEexp(group, es, true).prodPowP(bases)
        //println("///////////////////////////////////////////")

        // no work
        val vac = VACalg(group, es, true)
        val result = vac.prodPowP(bases)
        val check =  bases.mapIndexed { idx, it -> it powP es[idx] }.reduce { a, b -> (a * b) }
        assertEquals(check, result)
        println()
    }

    @Test
    fun testVACexample2() {
        val e0 = 1231130.toElementModQ(group)
        val e1 = 3462110.toElementModQ(group)
        val e2 = 5673241.toElementModQ(group)
        val es = listOf(e0, e1, e2)

        val bases = List(3) { group.gPowP(group.randomElementModQ()) }
        val check =  bases.mapIndexed { idx, it -> it powP es[idx] }.reduce { a, b -> (a * b) }

        // works
        //val feResult = FEexp(group, es, true).prodPowP(bases)
        //println("///////////////////////////////////////////")
        //assertEquals(check, feResult)

        // no work
        val vac = VACalg(group, es, true)
        val result = vac.prodPowP(bases)
        assertEquals(check, result)
        println()
    }

    @Test
    fun testVACexample3() {
        val es = listOf(
            1231130.toElementModQ(group),
            3462110.toElementModQ(group),
            5673241.toElementModQ(group),
            2983477.toElementModQ(group),
            6345902.toElementModQ(group),
            329756.toElementModQ(group),
        )

        val bases = List(es.size) { group.gPowP(group.randomElementModQ()) }
        val check =  bases.mapIndexed { idx, it -> it powP es[idx] }.reduce { a, b -> (a * b) }

        // works
        val feResult = FEexp(group, es, true).prodPowP(bases)
        println("///////////////////////////////////////////")
        assertEquals(check, feResult)

        // no work
        val vac = VACalg(group, es, true)
        val result = vac.prodPowP(bases)
        assertEquals(check, result)
        println()
    }

    @Test
    fun testVACsizes() {
        runVAC(3, true)
    }

    fun runVAC(nrows : Int, show: Boolean = false) {
        val exps = List(nrows) { group.randomElementModQ() }
        val bases = List(nrows) { group.gPowP( group.randomElementModQ()) }

        val vac = VACalg(group, exps, show)
        val result = vac.prodPowP(bases)

        val check =  bases.mapIndexed { idx, it -> it powP exps[idx] }.reduce { a, b -> (a * b) }
        assertEquals(check, result)
    }

/*
    @Test
    fun testFEtiming() {
        val k = 12
        runFEtiming(k, 1)
        runFEtiming(k, 10)
        runFEtiming(k, 50)
        runFEtiming(k, 100)
        // runFEtiming(k, 1000)
        //runFEtiming(k, 100)
        //runFEtiming(k, 1000)
    }

    @Test
    fun testFEfork() {
        runFEtiming(8, 100)
        runFEtiming(12, 100)
        runFEtiming(16, 100)
    }

    fun runFEtiming(nexps : Int, nbases: Int) {
        println("runFEtiming nexps = $nexps, nbases = $nbases")

        val exps = List(nexps) { group.randomElementModQ() }

        var starting11 = getSystemTimeInMillis()
        val fe = FEexp(group, exps)
        val time11 = getSystemTimeInMillis() - starting11

        var starting12 = getSystemTimeInMillis()
        var countFE = 0

        repeat (nbases) {
            val bases = List(nexps) { group.gPowP(group.randomElementModQ()) }
            fe.prodPowP(bases)
            countFE += bases.size
        }
        val time12 = getSystemTimeInMillis() - starting12

        var starting = getSystemTimeInMillis()
        var countP = 0
        repeat (nbases) {
            val bases = List(nexps) { group.gPowP(group.randomElementModQ()) }
            bases.mapIndexed { idx, it ->
                countP++
                it powP exps[idx] }.reduce { a, b -> (a * b) }
        }
        val timePowP = getSystemTimeInMillis() - starting

        // println(" countFE = $countFE countP = $countP")
        val ratio = (time11 + time12).toDouble()/timePowP
        println(" timeFE = $time11 + $time12 = ${time11 + time12}, timePowP = $timePowP ratio = ${ratio.sigfig(2)}")
    }


    //////////////////////////////////////////////////////////////
    @Test
    fun testGenK() {
        KProducts(3).addKProducts()
        //addKProducts(3)
        //addKProducts(8)
    }

    //////////////////////////////////////////////////////////////
    @Test
    fun testVac() {
        runVac(5)
        runVac(6)
        runVac(7)
        runVac(8)
        runVac(9)
        runVac(10)
        runVac(11)
        runVac(12)
        runVac(13)
        runVac(14)
        runVac(15)
        runVac(16)
    }

    fun runVac(k : Int) {
        val exps = List(k) { group.randomElementModQ() }
        val fe = FEexp(group, exps, false)
    }
    */
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