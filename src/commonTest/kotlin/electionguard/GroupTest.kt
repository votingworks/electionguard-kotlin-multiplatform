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
    fun basicsSm() = basics { testGroup() }

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
    fun comparisonOperationsSm() = comparisonOperations { testGroup() }

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
    fun generatorsWorkSm() = generatorsWork { testGroup() }

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
    fun validResiduesForGPowPSm() = validResiduesForGPowP { testGroup() }

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
    fun additionBasicsSm() = additionBasics { testGroup() }

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
    fun additionWrappingQSm() = additionWrappingQ { testGroup() }

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
    fun multiplicationBasicsPSm() = multiplicationBasicsP { testGroup() }

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
    fun multiplicationBasicsQsm() = multiplicationBasicsQ { testGroup() }

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
    fun subtractionBasicsSm() = subtractionBasics { testGroup() }

    fun subtractionBasics(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            checkAll(
                elementsModQNoZero(context),
                elementsModQNoZero(context),
                elementsModQNoZero(context)
            ) { a, b, c ->
                assertEquals(a, a - context.ZERO_MOD_Q)
                assertEquals(a - b, -(b - a))
                assertEquals(a - (b - c), (a - b) + c)
            }
        }
    }

    @Test
    fun negationLg() = negation { productionGroup() }

    @Test
    fun negationSm() = negation { testGroup() }

    fun negation(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            forAll(elementsModQ(context)) { context.ZERO_MOD_Q == (-it) + it }
        }
    }

    @Test
    fun multiplicativeInversesPLg() = multiplicativeInversesP { productionGroup() }

    @Test
    fun multiplicativeInversesPSm() = multiplicativeInversesP { testGroup() }

    fun multiplicativeInversesP(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            // our inverse code only works for elements in the subgroup, which makes it faster
            forAll(validElementsModP(context)) { it.multInv() * it == context.ONE_MOD_P }
        }
    }

    @Test
    fun multiplicativeInversesQ() {
        runTest {
            val context = productionGroup()
            forAll(elementsModQNoZero(context)) { it.multInv() * it == context.ONE_MOD_Q }
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
    fun exponentiationLg() = exponentiation { productionGroup() }

    @Test
    fun exponentiationSm() = exponentiation { testGroup() }

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
    fun subgroupInversesSm() = subgroupInverses { testGroup() }

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
}