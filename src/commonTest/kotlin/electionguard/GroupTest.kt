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
    val context = productionGroup()
    val smContext = testGroup()

    @Test
    fun basicsLg() = basics(context)

    @Test
    fun basicsSm() = basics(smContext)

    fun basics(context: GroupContext) {
        val three = 3.toElementModQ(context)
        val four = 4.toElementModQ(context)
        val seven = 7.toElementModQ(context)
        assertEquals(seven, three + four)
    }

    @Test
    fun comparisonOperationsLg() = comparisonOperations(context)

    @Test
    fun comparisonOperationsSm() = comparisonOperations(smContext)

    fun comparisonOperations(context: GroupContext) {
        val three = 3.toElementModQ(context)
        val four = 4.toElementModQ(context)

        assertTrue(three < four)
        assertTrue(three <= four)
        assertTrue(four > three)
        assertTrue(four >= four)
    }

    @Test
    fun generatorsWorkLg() = generatorsWork(context)

    @Test
    fun generatorsWorkSm() = generatorsWork(smContext)

    fun generatorsWork(context: GroupContext) {
        runProperty {
            forAll(elementsModP(context)) { it.inBounds() }
            forAll(elementsModQ(context)) { it.inBounds() }
        }
    }

    @Test
    fun validResiduesForGPowPLg() = validResiduesForGPowP(context)

    @Test
    fun validResiduesForGPowPSm() = validResiduesForGPowP(smContext)

    fun validResiduesForGPowP(context: GroupContext) {
        runProperty {
            forAll(propTestFastConfig, validElementsModP(context)) { it.isValidResidue() }
        }
    }

    @Test
    fun binaryArrayRoundTrip() {
        runProperty {
            forAll(elementsModP(context)) { it == context.binaryToElementModP(it.byteArray()) }
            forAll(elementsModQ(context)) { it == context.binaryToElementModQ(it.byteArray()) }
        }
    }

    @Test
    fun base64RoundTrip() {
        runProperty {
            forAll(elementsModP(context)) { it == context.base64ToElementModP(it.base64()) }
            forAll(elementsModQ(context)) { it == context.base64ToElementModQ(it.base64()) }
        }
    }

    @Test
    fun baseConversionFails() {
        listOf("", "@@", "-10", "1234567890".repeat(1000))
            .forEach {
                assertNull(context.base64ToElementModP(it))
                assertNull(context.base64ToElementModQ(it))
            }
    }

    @Test
    fun additionBasicsLg() = additionBasics(context)

    @Test
    fun additionBasicsSm() = additionBasics(smContext)

    fun additionBasics(context: GroupContext) {
        runProperty {
            checkAll(elementsModQ(context), elementsModQ(context), elementsModQ(context))
                { a, b, c ->
                    assertEquals(a, a + context.ZERO_MOD_Q) // identity
                    assertEquals(a + b, b + a) // commutative
                    assertEquals(a + (b + c), (a + b) + c) // associative
                }
        }
    }

    @Test
    fun additionWrappingQLg() = additionWrappingQ(context)

    @Test
    fun additionWrappingQSm() = additionWrappingQ(smContext)

    fun additionWrappingQ(context: GroupContext) {
        runProperty {
            checkAll(Arb.int(min=0, max=intTestQ - 1)) { i ->
                val iq = i.toElementModQ(context)
                val q = context.ZERO_MOD_Q - iq
                assertTrue(q.inBounds())
                assertEquals(context.ZERO_MOD_Q, q + iq)
            }
        }
    }

    @Test
    fun multiplicationBasicsPLg() = multiplicationBasicsP(context)

    @Test
    fun multiplicationBasicsPSm() = multiplicationBasicsP(smContext)

    fun multiplicationBasicsP(context: GroupContext) {
        runProperty {
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
    fun multiplicationBasicsQLg() = multiplicationBasicsQ(context)

    @Test
    fun multiplicationBasicsQsm() = multiplicationBasicsQ(smContext)

    fun multiplicationBasicsQ(context: GroupContext) {
        runProperty {
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
    fun subtractionBasicsLg() = subtractionBasics(context)

    @Test
    fun subtractionBasicsSm() = subtractionBasics(smContext)

    fun subtractionBasics(context: GroupContext) {
        runProperty {
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
    fun negationLg() = negation(context)

    @Test
    fun negationSm() = negation(smContext)

    fun negation(context: GroupContext) {
        runProperty { forAll(elementsModQ(context)) { context.ZERO_MOD_Q == (-it) + it } }
    }

    @Test
    fun multiplicativeInversesPLg() = multiplicativeInversesP(context)

    @Test
    fun multiplicativeInversesPSm() = multiplicativeInversesP(smContext)

    fun multiplicativeInversesP(context: GroupContext) {
        runProperty {
            // our inverse code only works for elements in the subgroup, which makes it faster
            forAll(validElementsModP(context)) { it.multInv() * it == context.ONE_MOD_P }
        }
    }

    @Test
    fun multiplicativeInversesQ() {
        runProperty {
            forAll(elementsModQNoZero(context)) { it.multInv() * it == context.ONE_MOD_Q }
        }
    }

    @Test
    fun divisionP() {
        runProperty {
            forAll(validElementsModP(context), validElementsModP(context)) { a, b ->
                (a * b) / b == a // division undoes multiplication
            }
        }
    }

    @Test
    fun exponentiationLg() = exponentiation(context)

    @Test
    fun exponentiationSm() = exponentiation(smContext)

    fun exponentiation(context: GroupContext) {
        runProperty {
            forAll(propTestFastConfig, elementsModQ(context), elementsModQ(context)) { a, b ->
                context.gPowP(a) * context.gPowP(b) == context.gPowP(a + b)
            }
        }
    }

    @Test
    fun acceleratedExponentiation() {
        runProperty {
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
    fun subgroupInversesLg() = subgroupInverses(context)

    @Test
    fun subgroupInversesSm() = subgroupInverses(smContext)

    fun subgroupInverses(context: GroupContext) {
        runProperty {
            forAll(propTestFastConfig, elementsModQ(context)) {
                val p1 = context.gPowP(it)
                val p2 = p1 powP (context.ZERO_MOD_Q - context.ONE_MOD_Q)
                p1 * p2 == context.ONE_MOD_P
            }
        }
    }
}