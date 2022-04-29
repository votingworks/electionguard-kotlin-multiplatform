package electionguard.workflow

import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.core.toElementModQ
import kotlin.test.Test
import kotlin.test.assertEquals

class LagrangeCoefficientsTest {
    val group = productionGroup()

    @Test
    fun testLagrangeCoefficientAreIntegral() {
        testLagrangeCoefficientAreIntegral(listOf(1, 2, 3))
        testLagrangeCoefficientAreIntegral(listOf(1, 2, 3, 4))
        testLagrangeCoefficientAreIntegral(listOf(2, 3, 4))
        testLagrangeCoefficientAreIntegral(listOf(2, 4, 5), false)
        testLagrangeCoefficientAreIntegral(listOf(2, 3, 4, 5))
        testLagrangeCoefficientAreIntegral(listOf(5, 6, 7, 8, 9))
        testLagrangeCoefficientAreIntegral(listOf(2, 3, 4, 5, 6, 7, 8, 9))
        testLagrangeCoefficientAreIntegral(listOf(2, 3, 4, 5, 6, 7, 9))
        testLagrangeCoefficientAreIntegral(listOf(2, 3, 4, 5, 6, 9), false)
        testLagrangeCoefficientAreIntegral(listOf(2, 3, 4, 5, 6, 7, 11), false)
    }

    fun testLagrangeCoefficientAreIntegral(coords: List<Int>, exact: Boolean = true) {
        println(coords)
        for (coord in coords) {
            val others: List<Int> = coords.filter { !it.equals(coord) }
            val coeff: Int = computeLagrangeCoefficientInt(coord, others)
            val numer: Int = computeLagrangeNumerator(coord, others)
            val denom: Int = computeLagrangeDenominator(coord, others)
            val coeffQ = computeLagrangeCoefficientQ(group, coord, others)
            println("($coord) $coeff == ${numer} / ${denom} rem ${numer % denom} == $coeffQ")
            if (exact) {
                assertEquals(0, numer % denom)
            }
        }
        println()
    }
}

fun computeLagrangeCoefficientInt(coordinate: Int, others: List<Int>): Int {
    val numerator: Int = others.reduce { a, b -> a * b }

    val diff: List<Int> = others.map { degree: Int -> degree - coordinate }
    val denominator = diff.reduce { a, b -> a * b }

    return numerator / denominator
}

fun computeLagrangeNumerator(coordinate: Int, others: List<Int>): Int {
    return others.reduce { a, b -> a * b }
}

fun computeLagrangeDenominator(coordinate: Int, others: List<Int>): Int {
    val diff: List<Int> = others.map { degree: Int -> degree - coordinate }
    return diff.reduce { a, b -> a * b }
}

fun computeLagrangeCoefficientQ(group: GroupContext, coordinate: Int, degrees: List<Int>): ElementModQ {
    val numerator: Int = degrees.reduce { a, b -> a * b }

    val diff: List<Int> = degrees.map { degree -> degree.toInt() - coordinate.toInt() }
    val denominator = diff.reduce { a, b -> a * b }

    val denomQ =
        if (denominator > 0) denominator.toElementModQ(group) else (-denominator).toElementModQ(group).unaryMinus()

    return numerator.toElementModQ(group) / denomQ
}