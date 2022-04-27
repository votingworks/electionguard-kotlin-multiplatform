package electionguard.workflow

import kotlin.test.Test
import kotlin.test.assertEquals

class LagrangeCoefficientsTest {
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
}

fun testLagrangeCoefficientAreIntegral(coords: List<Int>, exact: Boolean = true) {
    println(coords)
    for (coord in coords) {
        val others: List<Int> = coords.filter { !it.equals(coord) }
        val coeff: Int = computeLagrangeCoefficient(coord, others)
        val numer: Int = computeLagrangeNumerator(coord, others)
        val denom: Int = computeLagrangeDenominator(coord, others)
        println("($coord) $coeff == ${numer} / ${denom} rem ${numer % denom}")
        if (exact) {
            assertEquals(0, numer % denom)
        }
    }
    println()
}

fun computeLagrangeCoefficient(coordinate: Int, degrees: List<Int>): Int {
    val numerator: Int = degrees.reduce { a, b -> a * b }

    val diff: List<Int> = degrees.map { degree: Int -> degree - coordinate }
    val denominator = diff.reduce { a, b -> a * b }

    return numerator / denominator
}

fun computeLagrangeNumerator(coordinate: Int, degrees: List<Int>): Int {
    return degrees.reduce { a, b -> a * b }
}

fun computeLagrangeDenominator(coordinate: Int, degrees: List<Int>): Int {
    val diff: List<Int> = degrees.map { degree: Int -> degree - coordinate }
    return diff.reduce { a, b -> a * b }
}