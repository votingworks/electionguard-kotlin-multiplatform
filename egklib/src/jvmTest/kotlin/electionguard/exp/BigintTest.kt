package electionguard.exp

import electionguard.core.*
import electionguard.util.sigfig
import org.cryptobiotic.bigint.BigInteger
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BigintTest {
    val group = productionGroup()

    @Test
    fun testMultiply() {
        timeMultiply(1000)
        timeMultiply(10000)
        timeMultiply(20000)
    }

    // all with java.math.BigInteger: time multiplyMod, square and multiply()
    fun timeMultiply(n:Int) {
        val modulus = java.math.BigInteger(1, group.constants.largePrime)

        val nonces = List(n) { group.randomElementModQ() }
        val elemps = nonces.map { group.gPowP(it) }
        val basesb = elemps.map { it.toBigM() }

        var starting = getSystemTimeInMillis()
        val prod = basesb.reduce { a, b -> a.multiply(b).mod(modulus) }
        var duration = getSystemTimeInMillis() - starting
        var peracc = duration.toDouble() / n
        println("multiplyMod took $duration msec for $n = $peracc msec per multiply")

        starting = getSystemTimeInMillis()
        basesb.forEach { it.multiply(it).mod(modulus) }
        duration = getSystemTimeInMillis() - starting
        peracc = duration.toDouble() / n
        println("squareMod took $duration msec for $n = $peracc msec per square")

        starting = getSystemTimeInMillis()
        basesb.forEach { it.multiply(it) }
        duration = getSystemTimeInMillis() - starting
        peracc = duration.toDouble() / n
        println("square took $duration msec for $n = $peracc msec per multiply")
    }

    @Test
    // test counting ops for runProdPowB, small in that exponents < 5 bits
    fun testProdPowBSmall() {
        val e0 = 30.toElementModQ(group)
        val e1 = 10.toElementModQ(group)
        val e2 = 24.toElementModQ(group)
        val es = listOf(e0, e1, e2)

        val bases = List(3) { group.gPowP(group.randomElementModQ()) }

        val org: ElementModP = bases.mapIndexed { idx, it -> it powP es[idx] }.reduce { a, b -> (a * b) }
        val orgb = org.toBig()

        val productb = runProdPowB(es, bases, group, true)
        assertEquals(orgb, productb)
    }

    @Test
    fun testCountProdPowB() {
        countProdPowB(1)
        countProdPowB(10)
        countProdPowB(100)
        countProdPowB(1000)
        countProdPowB(100)
        countProdPowB(1000)
        countProdPowB(100)
    }

    // count ops for runProdPowB, full size exponents (256 bits), check result is correct
    // get time for standard runProdPowB (using java.math.BigInteger)
    fun countProdPowB(nexps: Int) {
        val es = List(nexps) { group.randomElementModQ() }
        val bases = List(nexps) { group.gPowP(group.randomElementModQ()) }

        var starting = getSystemTimeInMillis()
        val org: ElementModP = bases.mapIndexed { idx, it -> it powP es[idx] }.reduce { a, b -> (a * b) }
        val took = getSystemTimeInMillis() - starting
        val perN = took.toDouble() / nexps

        println("*** prodPow nrows = $nexps took ${took} msec, ${perN.sigfig(2)} msecs per row")
        val orgb = org.toBig()

        val productb = runProdPowB(es, bases, group, true)
        assertEquals(orgb, productb)
    }

    @Test
    fun testBigintQQ() {
        compareTimeProdPow(1)
        compareTimeProdPow(3)
        compareTimeProdPow(10)
        compareTimeProdPow(100)
        compareTimeProdPow(1000)
    }

    // time standard ProdPow, vs ProdPowB
    fun compareTimeProdPow(nexps: Int) {
        println("nexps = $nexps")
        val es = List(nexps) { group.randomElementModQ() }
        val bases = List(nexps) { group.gPowP(group.randomElementModQ()) }
        var starting = getSystemTimeInMillis()
        val org: ElementModP = bases.mapIndexed { idx, it -> it powP es[idx] }.reduce { a, b -> (a * b) }
        val orgb: BigInteger = org.toBig()
        val timePowQ = getSystemTimeInMillis() - starting

        starting = getSystemTimeInMillis()
        val esb = es.map { it.toBig() }
        val basesb = bases.map { it.toBig() }
        val modulus = BigInteger(1, group.constants.largePrime)
        val expsb = basesb.mapIndexed { idx, it -> it.modPow(esb[idx], modulus) }
        val productb: BigInteger = expsb.reduce { a, b -> (a.multiply(b)).mod(modulus) }
        val timePowQQ = getSystemTimeInMillis() - starting

        assertEquals(orgb, productb)

        val ratio = timePowQQ.toDouble() / timePowQ
        println(" compareTimeProdPow = $timePowQ, timePowQQ = $timePowQQ timePowQQ/timePowQ = ${ratio.sigfig(2)}")
    }
}

fun runProdPowB(exps: List<ElementModQ>, bases: List<ElementModP>, group: GroupContext, show: Boolean = false): BigInteger {
    val modulus = BigInteger(1, group.constants.largePrime)
    val esb = exps.map { it.toBig() }
    val basesb = bases.map { it.toBig() }
    return runProdPowB(esb, basesb, modulus, show)
}

// use BigIntegerB to count operations for prodModPow (product of exponentiations), n bases and n exponents
fun runProdPowB(exps: List<BigInteger>, bases: List<BigInteger>, modulus: BigInteger, show: Boolean = false): BigInteger {
    // modPow(BigInteger exponent, BigInteger modulus)
    BigInteger.getAndClearOpCounts()
    val expsb = bases.mapIndexed { idx, it -> it.modPow(exps[idx], modulus) }
    if (show) println(showCountResults(" BigIntegerB.modPow"))
    // this.element * other.getCompat(groupContext)).modWrap()
    val productb: BigInteger = expsb.reduce { a, b -> (a.multiply(b)).mod(modulus) }
    if (show) println(showCountResults(" BigIntegerB.multiply"))
    return productb
}

fun showCountResults(where: String): String {
    val opCounts = BigInteger.getAndClearOpCounts()
    return buildString {
        appendLine("$where:")
        opCounts.toSortedMap().forEach{ (key, value) ->
            appendLine("   $key : $value")
        }
    }
}

