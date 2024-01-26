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
        val k = 12
        runFEtiming(k, 1)
        runFEtiming(k, 10)
        //runFEtiming(k, 100)
        //runFEtiming(k, 1000)
    }

    fun runFEtiming(nrows : Int, nbases: Int) {
        println("runFEtiming nrows = $nrows, nbases = $nbases")

        val exps = List(nrows) { group.randomElementModQ() }

        var starting11 = getSystemTimeInMillis()
        val fe = FEexp(group, exps)
        val time11 = getSystemTimeInMillis() - starting11

        var starting12 = getSystemTimeInMillis()
        var countFE = 0

        repeat (nbases) {
            val bases = List(nrows) { group.gPowP(group.randomElementModQ()) }
            fe.prodPowP(bases)
            countFE += bases.size
        }
        val time12 = getSystemTimeInMillis() - starting12

        var starting = getSystemTimeInMillis()
        var countP = 0
        repeat (nbases) {
            val bases = List(nrows) { group.gPowP(group.randomElementModQ()) }
            bases.mapIndexed { idx, it ->
                countP++
                it powP exps[idx] }.reduce { a, b -> (a * b) }
        }
        val timePowP = getSystemTimeInMillis() - starting

        println(" countFE = $countFE countP = $countP")
        println(" timeFE = $time11 + $time12 = ${time11 + time12}, timePowP = $timePowP")
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
        val voc = fe.vaChain
        println("runVac k = $k chain = ${voc.w.size}")

        val checkUse = mutableMapOf<Int, UseStat>()
        repeat(voc.w.size) { checkUse[it] = UseStat(it) }

        val elemMapByIndex : Map<Int, AdditionChain> = voc.elemMap.map { it.value.index to it.value }.toMap()

        voc.w.forEach{
            checkUse[it.first]?.count = checkUse[it.first]?.count!! + 1
            checkUse[it.second]?.count = checkUse[it.second]?.count!! + 1
        }

        if (false) {
            println("  w checkUse = ${checkUse.size}")
            println(buildString {
                checkUse.forEach { entry ->
                    println("  ${entry.key} = ${entry.value.count}")
                }
            })
        }

        var missing = 0
        val sortit = checkUse.values.toList().sorted()
        println(buildString {
            sortit.forEach { stat ->
                if (stat.count == 0) {
                    val ac = elemMapByIndex[stat.idx]
                    //if (ac == null) println("missing index ${stat.idx}")
                    //else println("  ${stat.idx}: unused ${ac} count = ${ac.elems.sum()}")
                    missing++
                }
            }
        })
        println("  total ws unused=${missing} = ${missing.toDouble()/voc.w.size}\n")
    }

    data class UseStat(val idx: Int) : Comparable<UseStat> {
        var count = 0
        override fun compareTo(other: UseStat): Int {
            return this.idx - other.idx
        }
    }
}