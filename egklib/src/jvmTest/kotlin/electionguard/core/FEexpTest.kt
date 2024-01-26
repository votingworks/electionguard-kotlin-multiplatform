package electionguard.core

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FEexpTest {
    val group = productionGroup()

    @Test
    fun testFEexample() {
        val e0 = 30.toElementModQ(group)
        val e1 = 10.toElementModQ(group)
        val e2 = 24.toElementModQ(group)
        val es = listOf(e0, e1, e2)

        val bases = List(3) { group.gPowP( group.randomElementModQ()) }

        val fe = FEexp(group, es)
        val result = fe.prodPowP(bases)

        val check =  bases.mapIndexed { idx, it -> it powP es[idx] }.reduce { a, b -> (a * b) }
        assertEquals(check, result)
        println()
    }

    @Test
    fun testSMsizes() {
        runFM(3, true)
    }

    fun runFM(nrows : Int, show: Boolean = false) {
        val exps = List(nrows) { group.randomElementModQ() }
        val bases = List(nrows) { group.gPowP( group.randomElementModQ()) }

        val fe = FEexp(group, exps, show)
        val result = fe.prodPowP(bases)

        val check =  bases.mapIndexed { idx, it -> it powP exps[idx] }.reduce { a, b -> (a * b) }
        assertEquals(check, result)
    }

    @Test
    fun testFEtiming() {
        runFEtiming(3, 1)
        runFEtiming(3, 10)
        runFEtiming(3, 100)
        runFEtiming(3, 1000)
    }

    fun runFEtiming(nrows : Int, nbases: Int) {
        println("runSMrepeat nrows = $nrows, nbases = $nbases")

        val exps = List(nrows) { group.randomElementModQ() }
        val bases = List(nrows) { group.gPowP(group.randomElementModQ()) }

        var starting11 = getSystemTimeInMillis()
        val fe = FEexp(group, exps)
        val time11 = getSystemTimeInMillis() - starting11

        var starting12 = getSystemTimeInMillis()
        repeat (nbases) {
            fe.prodPowP(bases)
        }
        val time12 = getSystemTimeInMillis() - starting12

        var starting = getSystemTimeInMillis()
        repeat (nbases) {
            bases.mapIndexed { idx, it -> it powP exps[idx] }.reduce { a, b -> (a * b) }
        }
        val timePowP = getSystemTimeInMillis() - starting

        println(" timeSM = $time11 + $time12 = ${time11 + time12}, timePowP = $timePowP")
    }

}