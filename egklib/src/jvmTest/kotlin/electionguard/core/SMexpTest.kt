package electionguard.core

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SMexpTest {
    val group = productionGroup()

    @Test
    fun testSMexample() {
        val e0 = 30.toElementModQ(group)
        val e1 = 10.toElementModQ(group)
        val e2 = 24.toElementModQ(group)
        val es = listOf(e0, e1, e2)

        val bases = List(3) { group.gPowP( group.randomElementModQ()) }

        val result = SMexp(group, bases, es).prodPowP()

        val check =  bases.mapIndexed { idx, it -> it powP es[idx] }.reduce { a, b -> (a * b) }

        assertEquals(check, result)
    }

    @Test
    fun testSMsizes() {
        runSM(10)
        runSM(30)
        runSM(100)
    }

    fun runSM(nrows : Int) {
        val exps = List(nrows) { group.randomElementModQ() }
        val bases = List(nrows) { group.gPowP( group.randomElementModQ()) }

        val result = SMexp(group, bases, exps).prodPowP()
        val check =  bases.mapIndexed { idx, it -> it powP exps[idx] }.reduce { a, b -> (a * b) }
        assertEquals(check, result)
    }

}