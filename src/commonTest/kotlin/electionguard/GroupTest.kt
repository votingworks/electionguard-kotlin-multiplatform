package electionguard

import io.kotest.property.checkAll
import io.kotest.property.forAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupTest {
    val context = productionGroup()

    @Test
    fun basics() {
        val three = 3.toElementModQ(context)
        val four = 4.toElementModQ(context)
        val seven = 7.toElementModQ(context)
        assertEquals(seven, three + four)
    }

    @Test
    fun comparisonOperations() {
        val three = 3.toElementModQ(context)
        val four = 4.toElementModQ(context)

        assertTrue(three < four)
        assertTrue(three <= four)
        assertTrue(four > three)
        assertTrue(four >= four)
    }

    @Test
    fun generatorsWork() {
        runProperty {
            forAll(elementsModP(context)) { it.inBounds() }
            forAll(elementsModQ(context)) { it.inBounds() }
        }
    }

    @Test
    fun validResiduesForGPowP() {
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
    fun additionBasics() {
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
    fun multiplicationBasicsP() {
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
    fun multiplicationBasicsQ() {
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
    fun subtractionBasics() {
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
    fun negation() {
        runProperty { forAll(elementsModQ(context)) { context.ZERO_MOD_Q == (-it) + it } }
    }

    @Test
    fun multiplicativeInversesP() {
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
    fun exponentiation() {
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
    fun subgroupInverses() {
        runProperty {
            forAll(propTestFastConfig, elementsModQ(context)) {
                val p1 = context.gPowP(it)
                val p2 = p1 powP (context.ZERO_MOD_Q - context.ONE_MOD_Q)
                p1 * p2 == context.ONE_MOD_P
            }
        }
    }
}