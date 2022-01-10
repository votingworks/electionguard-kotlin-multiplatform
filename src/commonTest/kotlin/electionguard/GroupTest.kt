package electionguard

import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.kotest.property.forAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupTest {
    @Test
    fun basicsLg() = basics { productionGroup() }

    @Test
    fun basicsSm() = basics { tinyGroup() }

    fun basics(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            val three = 3.toElementModQ(context)
            val four = 4.toElementModQ(context)
            val seven = 7.toElementModQ(context)
            assertEquals(seven, three + four)
        }
    }

    @Test
    fun comparisonOperationsLg() = comparisonOperations { productionGroup() }

    @Test
    fun comparisonOperationsSm() = comparisonOperations { tinyGroup() }

    fun comparisonOperations(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            val three = 3.toElementModQ(context)
            val four = 4.toElementModQ(context)

            assertTrue(three < four)
            assertTrue(three <= four)
            assertTrue(four > three)
            assertTrue(four >= four)
        }
    }

    @Test
    fun generatorsWorkLg() = generatorsWork { productionGroup() }

    @Test
    fun generatorsWorkSm() = generatorsWork { tinyGroup() }

    fun generatorsWork(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            forAll(elementsModP(context)) { it.inBounds() }
            forAll(elementsModQ(context)) { it.inBounds() }
        }
    }

    @Test
    fun validResiduesForGPowPLg() = validResiduesForGPowP { productionGroup() }

    @Test
    fun validResiduesForGPowPSm() = validResiduesForGPowP { tinyGroup() }

    fun validResiduesForGPowP(contextF: suspend () -> GroupContext) {
        runTest {
            forAll(propTestFastConfig, validElementsModP(contextF())) { it.isValidResidue() }
        }
    }

    @Test
    fun binaryArrayRoundTrip() {
        runTest {
            val context = productionGroup()
            forAll(elementsModP(context)) { it == context.binaryToElementModP(it.byteArray()) }
            forAll(elementsModQ(context)) { it == context.binaryToElementModQ(it.byteArray()) }
        }
    }

    @Test
    fun base64RoundTrip() {
        runTest {
            val context = productionGroup()
            forAll(elementsModP(context)) { it == context.base64ToElementModP(it.base64()) }
            forAll(elementsModQ(context)) { it == context.base64ToElementModQ(it.base64()) }
        }
    }

    @Test
    fun baseConversionFails() {
        runTest {
            val context = productionGroup()
            listOf("", "@@", "-10", "1234567890".repeat(1000))
                .forEach {
                    assertNull(context.base64ToElementModP(it))
                    assertNull(context.base64ToElementModQ(it))
                }
        }
    }

    @Test
    fun additionBasicsLg() = additionBasics { productionGroup() }

    @Test
    fun additionBasicsSm() = additionBasics { tinyGroup() }

    fun additionBasics(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            checkAll(elementsModQ(context), elementsModQ(context), elementsModQ(context))
                { a, b, c ->
                    assertEquals(a, a + context.ZERO_MOD_Q) // identity
                    assertEquals(a + b, b + a) // commutative
                    assertEquals(a + (b + c), (a + b) + c) // associative
                }
        }
    }

    @Test
    fun additionWrappingQLg() = additionWrappingQ { productionGroup() }

    @Test
    fun additionWrappingQSm() = additionWrappingQ { tinyGroup() }

    fun additionWrappingQ(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            checkAll(Arb.int(min=0, max=intTestQ - 1)) { i ->
                val iq = i.toElementModQ(context)
                val q = context.ZERO_MOD_Q - iq
                assertTrue(q.inBounds())
                assertEquals(context.ZERO_MOD_Q, q + iq)
            }
        }
    }

    @Test
    fun multiplicationBasicsPLg() = multiplicationBasicsP { productionGroup() }

    @Test
    fun multiplicationBasicsPSm() = multiplicationBasicsP { tinyGroup() }

    fun multiplicationBasicsP(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            checkAll(
                elementsModPNoZero(context),
                elementsModPNoZero(context),
                elementsModPNoZero(context)
            ) { a, b, c ->
                assertEquals(a, a * context.ONE_MOD_P) // identity
                assertEquals(a * b, b * a) // commutative
                assertEquals(a * (b * c), (a * b) * c) // associative
            }
        }
    }

    @Test
    fun multiplicationBasicsQLg() = multiplicationBasicsQ { productionGroup() }

    @Test
    fun multiplicationBasicsQsm() = multiplicationBasicsQ { tinyGroup() }

    fun multiplicationBasicsQ(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            checkAll(
                elementsModQNoZero(context),
                elementsModQNoZero(context),
                elementsModQNoZero(context)
            ) { a, b, c ->
                assertEquals(a, a * context.ONE_MOD_Q)
                assertEquals(a * b, b * a)
                assertEquals(a * (b * c), (a * b) * c)
            }
        }
    }

    @Test
    fun subtractionBasicsLg() = subtractionBasics { productionGroup() }

    @Test
    fun subtractionBasicsSm() = subtractionBasics { tinyGroup() }

    fun subtractionBasics(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            checkAll(
                elementsModQNoZero(context),
                elementsModQNoZero(context),
                elementsModQNoZero(context)
            ) { a, b, c ->
                assertEquals(a, a - context.ZERO_MOD_Q, "identity")
                assertEquals(a - b, -(b - a), "commutativity-ish")
                assertEquals(a - (b - c), (a - b) + c, "associativity-ish")
            }
        }
    }

    @Test
    fun negationLg() = negation { productionGroup() }

    @Test
    fun negationSm() = negation { tinyGroup() }

    fun negation(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            forAll(elementsModQ(context)) { context.ZERO_MOD_Q == (-it) + it }
        }
    }

    @Test
    fun multiplicativeInversesPLg() = multiplicativeInversesP { productionGroup() }

    @Test
    fun multiplicativeInversesPSm() = multiplicativeInversesP { tinyGroup() }

    fun multiplicativeInversesP(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            // our inverse code only works for elements in the subgroup, which makes it faster
            forAll(propTestFastConfig, validElementsModP(context)) {
                it.multInv() * it == context.ONE_MOD_P
            }
        }
    }

    @Test
    fun multiplicativeInversesQLg() = multiplicativeInversesQ { productionGroup() }

    @Test
    fun multiplicativeInversesQSm() = multiplicativeInversesQ { tinyGroup() }

    fun multiplicativeInversesQ(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            checkAll(elementsModQNoZero(context)) {
                assertEquals(context.ONE_MOD_Q, it.multInv() * it)
            }
        }
    }

    @Test
    fun divisionP() {
        runTest {
            val context = productionGroup()
            forAll(validElementsModP(context), validElementsModP(context)) { a, b ->
                (a * b) / b == a // division undoes multiplication
            }
        }
    }

    @Test
    fun exponentiationQLg() = exponentiationQ { productionGroup() }

    @Test
    fun exponentiationQSm() = exponentiationQ { tinyGroup() }

    fun exponentiationQ(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            checkAll(propTestFastConfig, elementsModQ(context), elementsModQ(context), elementsModQ(context)) { a, b, c ->
                assertEquals(a powQ (b + c), (a powQ b) * (a powQ c))
            }
        }
    }

    @Test
    fun exponentiationLg() = exponentiation { productionGroup() }

    @Test
    fun exponentiationSm() = exponentiation { tinyGroup() }

    fun exponentiation(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            forAll(propTestFastConfig, elementsModQ(context), elementsModQ(context)) { a, b ->
                context.gPowP(a) * context.gPowP(b) == context.gPowP(a + b)
            }
        }
    }

    @Test
    fun acceleratedExponentiation() {
        runTest {
            val context = productionGroup()
            forAll(propTestFastConfig, elementsModQ(context), elementsModQ(context)) { a, b ->
                val ga = context.gPowP(a)
                val normal = ga powP b
                val gaAccelerated = ga.acceleratePow()
                val faster = gaAccelerated powP b
                normal == faster
            }
        }
    }

    @Test
    fun subgroupInversesLg() = subgroupInverses { productionGroup() }

    @Test
    fun subgroupInversesSm() = subgroupInverses { tinyGroup() }

    fun subgroupInverses(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            forAll(propTestFastConfig, elementsModQ(context)) {
                val p1 = context.gPowP(it)
                val p2 = p1 powP (context.ZERO_MOD_Q - context.ONE_MOD_Q)
                p1 * p2 == context.ONE_MOD_P
            }
        }
    }

    @Test
    fun iterableAdditionLg() = iterableAddition { productionGroup() }

    @Test
    fun iterableAdditionSm() = iterableAddition { tinyGroup() }

    fun iterableAddition(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            checkAll(
                propTestFastConfig,
                elementsModQ(context),
                elementsModQ(context),
                elementsModQ(context)
            ) { a, b, c ->
                val expected = a + b + c
                assertEquals(expected, context.addQ(a, b, c))
                assertEquals(expected, with(context) { listOf(a, b, c).addQ() })
            }
        }
    }

    @Test
    fun iterableMultiplicationLg() = iterableMultiplication { productionGroup() }

    @Test
    fun iterableMultiplicationSm() = iterableMultiplication { tinyGroup() }

    fun iterableMultiplication(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            checkAll(
                propTestFastConfig,
                validElementsModP(context),
                validElementsModP(context),
                validElementsModP(context)
            ) { a, b, c ->
                val expected = a * b * c
                assertEquals(expected, context.multP(a, b, c))
                assertEquals(expected, with(context) { listOf(a, b, c).multP() })
            }
        }
    }
}