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
        runFM(1)
    }

    fun runFM(nrows : Int) {
        val exps = List(nrows) { group.randomElementModQ() }
        val bases = List(nrows) { group.gPowP( group.randomElementModQ()) }

        val fe = FEexp(group, exps)
        val result = fe.prodPowP(bases)

        val check =  bases.mapIndexed { idx, it -> it powP exps[idx] }.reduce { a, b -> (a * b) }
        assertEquals(check, result)
    }

}