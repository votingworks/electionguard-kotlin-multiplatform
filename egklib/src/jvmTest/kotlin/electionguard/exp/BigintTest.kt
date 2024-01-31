package electionguard.exp

import electionguard.core.*
import electionguard.util.pad
import electionguard.util.sigfig
import org.cryptobiotic.bigint.BigInteger
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BigintTest {
    val group = productionGroup()

    @Test
    fun testBigintSmall() {
        val e0 = 30.toElementModQ(group)
        val e1 = 10.toElementModQ(group)
        val e2 = 24.toElementModQ(group)
        val es = listOf(e0, e1, e2)

        val bases = List(3) { group.gPowP(group.randomElementModQ()) }

        val org: ElementModP = bases.mapIndexed { idx, it -> it powP es[idx] }.reduce { a, b -> (a * b) }
        val orgb = org.toBig()

        val productb = runProdPow(es, bases, group)
        assertEquals(orgb, productb)
    }

    @Test
    fun testBigintQ() {
        runBigintQ(1)
        runBigintQ(3)
        runBigintQ(10)
        runBigintQ(100)
        runBigintQ(1000)
    }

    fun runBigintQ(nexps: Int) {
        println("nexps = $nexps")
        val es = List(nexps) { group.randomElementModQ() }
        val bases = List(nexps) { group.gPowP(group.randomElementModQ()) }

        val org: ElementModP = bases.mapIndexed { idx, it -> it powP es[idx] }.reduce { a, b -> (a * b) }
        val orgb = org.toBig()

        val productb = runProdPow(es, bases, group, true)
        assertEquals(orgb, productb)
    }

    @Test
    fun testBigintQQ() {
        runBigintQQ(1)
        runBigintQQ(3)
        runBigintQQ(10)
        runBigintQQ(100)
        runBigintQQ(1000)
    }

    fun runBigintQQ(nexps: Int) {
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
        println(" timePowQ = $timePowQ, timePowQQ = $timePowQQ timePowQQ/timePowQ = ${ratio.sigfig(2)}")

    }
}

fun runProdPow(exps: List<ElementModQ>, bases: List<ElementModP>, group: GroupContext, show: Boolean = false): BigInteger {
    val modulus = BigInteger(1, group.constants.largePrime)
    val esb = exps.map { it.toBig() }
    val basesb = bases.map { it.toBig() }
    return runProdPow(esb, basesb, modulus, show)
}

fun runProdPow(exps: List<BigInteger>, bases: List<BigInteger>, modulus: BigInteger, show: Boolean = false): BigInteger {
    // modPow(BigInteger exponent, BigInteger modulus)
    BigInteger.getAndClearCounts()
    val expsb = bases.mapIndexed { idx, it -> it.modPow(exps[idx], modulus) }
    if (show) println(showCountResults(" reg.modPow"))
    // this.element * other.getCompat(groupContext)).modWrap()
    val productb: BigInteger = expsb.reduce { a, b -> (a.multiply(b)).mod(modulus) }
    if (show) println(showCountResults(" reg.multiply"))
    return productb
}

fun showCountResults(where: String): String {
    val opCounts = BigInteger.getAndClearCounts()

    return buildString {
        appendLine("$where:")
        opCounts.toSortedMap().forEach{ (key, value) ->
            appendLine("   $key : $value")
        }
    }
}

// Enabled pure java code ends here
// Removed native code here.
/**
 * Theoretically optimal width of pre-computed table.
 *
 * @param bitLength Bit length of exponents used to compute
 * power-products.
 * @param size Number of exponentiations that will be computed.
 * @return Theoretical optimal width.
 */
fun optimalWidth(bitLength: Int, size: Int): Int {
    var width = 2
    var cost = 1.5 * bitLength
    var oldCost: Double
    do {
        oldCost = cost

        // Amortized cost for table.
        val t =
            (((1 shl width) - width + bitLength).toDouble()) / size

        // Cost for multiplication.
        val m = (bitLength.toDouble()) / width

        cost = t + m

        width++
    } while (width <= 16 && cost < oldCost)

    // We reduce the theoretical value by one to account for the
    // overhead.
    return width - 1
}
