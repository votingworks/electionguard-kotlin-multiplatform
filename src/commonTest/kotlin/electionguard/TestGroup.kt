package electionguard

import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.kotest.property.forAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GroupTest {
    @Test
    fun basicsP() {
        val ctx = lowMemoryProductionGroup()
        val one = 1.toElementModP(ctx)
        val two = 2.toElementModP(ctx)
        val alsoTwo = (1 + 1).toElementModP(ctx)
        val three = 3.toElementModP(ctx)
        val four = 4.toElementModP(ctx)
        val twelve = 12.toElementModP(ctx)

        assertEquals(one, one)
        assertEquals(two, alsoTwo)

        // basic multiplication stuff
        assertEquals(one, one * one)
        assertEquals(two, one * two)
        assertEquals(two, two * one)
        assertEquals(alsoTwo, two * one)
        assertEquals(four, two * two)
        assertEquals(twelve, three * four)
    }

    @Test
    fun basicsQ() {
        val ctx = lowMemoryProductionGroup()
        val one = 1.toElementModQ(ctx)
        val two = 2.toElementModQ(ctx)
        val alsoTwo = (1 + 1).toElementModQ(ctx)
        val three = 3.toElementModQ(ctx)
        val four = 4.toElementModQ(ctx)
        val twelve = 12.toElementModQ(ctx)

        assertEquals(one, one)
        assertEquals(two, alsoTwo)

        // basic multiplication
        assertEquals(one, one * one)
        assertEquals(two, one * two)
        assertEquals(two, two * one)
        assertEquals(alsoTwo, two * one)
        assertEquals(four, two * two)
        assertEquals(twelve, three * four)

        // basic addition
        assertEquals(two, one + one)
        assertEquals(three, one + two)
        assertEquals(four, two + two)
        assertEquals(twelve, four + three + three + two)
    }

    @Test
    fun comparisonOperations() {
        val ctx = lowMemoryProductionGroup()
        val two = 2.toElementModP(ctx)
        val three = 3.toElementModP(ctx)
        val four = 4.toElementModP(ctx)

        assertTrue(three < four)
        assertTrue(three <= four)
        assertTrue(four > three)
        assertTrue(four >= four)
        assertTrue(four > two)
    }

    @Test
    fun generatorsWorkSmall() {
        runProperty { forAll(elementsModP(testGroup())) { it.inBounds() } }

        runProperty { forAll(elementsModQ(testGroup())) { it.inBounds() } }
    }

    @Test
    fun generatorsWorkLarge() {
        runProperty {
            forAll(propTestFastConfig, elementsModP(lowMemoryProductionGroup())) { it.inBounds() }
        }

        runProperty {
            forAll(propTestFastConfig, elementsModQ(lowMemoryProductionGroup())) { it.inBounds() }
        }
    }

    @Test
    fun validResidues() {
        runProperty { forAll(validElementsModP(testGroup())) { it.isValidResidue() } }

        runProperty {
            forAll(propTestFastConfig, validElementsModP(lowMemoryProductionGroup())) {
                it.isValidResidue()
            }
        }
    }

    @Test
    fun binaryArrayRoundTripSmall() {
        runProperty {
            forAll(elementsModP()) { it == it.context.binaryToElementModP(it.byteArray()) }
        }

        runProperty {
            forAll(elementsModQ()) { it == it.context.binaryToElementModQ(it.byteArray()) }
        }
    }

    @Test
    fun binaryArrayRoundTripBig() {
        runProperty {
            forAll(propTestFastConfig, elementsModP(lowMemoryProductionGroup())) {
                it == it.context.binaryToElementModP(it.byteArray())
            }
        }

        runProperty {
            forAll(propTestFastConfig, elementsModQ(lowMemoryProductionGroup())) {
                it == it.context.binaryToElementModQ(it.byteArray())
            }
        }
    }

    @Test
    fun base64RoundTripSmall() {
        runProperty { forAll(elementsModP()) { it == it.context.base64ToElementModP(it.base64()) } }

        runProperty { forAll(elementsModQ()) { it == it.context.base64ToElementModQ(it.base64()) } }
    }

    @Test
    fun base64RoundTripBig() {
        runProperty {
            forAll(propTestFastConfig, elementsModP(lowMemoryProductionGroup())) {
                it == it.context.base64ToElementModP(it.base64())
            }
        }

        runProperty {
            forAll(propTestFastConfig, elementsModQ(lowMemoryProductionGroup())) {
                it == it.context.base64ToElementModQ(it.base64())
            }
        }
    }

    @Test
    fun commutativity() {
        runProperty {
            forAll(elementsModP(), elementsModP()) { a, b -> a * b == b * a }
            forAll(elementsModQ(), elementsModQ()) { a, b -> a * b == b * a }
            forAll(elementsModQ(), elementsModQ()) { a, b -> a + b == b + a }
        }
    }

    @Test
    fun associativity() {
        runProperty {
            forAll(elementsModP(), elementsModP(), elementsModP()) { a, b, c ->
                (a * b) * c == a * (b * c)
            }
            forAll(elementsModQ(), elementsModQ(), elementsModQ()) { a, b, c ->
                (a * b) * c == a * (b * c)
            }
            forAll(elementsModQ(), elementsModQ(), elementsModQ()) { a, b, c ->
                (a + b) + c == a + (b + c)
            }
        }
    }

    @Test
    fun distributive() {
        runProperty {
            forAll(elementsModQ(), elementsModQ(), elementsModQ()) { a, b, c ->
                (a + b) * c == a * c + b * c
            }
        }
    }

    @Test
    fun identity() {
        runProperty {
            forAll(elementsModQ()) { it + it.context.ZERO_MOD_Q == it }

            forAll(elementsModQ()) { it * it.context.ONE_MOD_Q == it }
            forAll(elementsModP()) { it * it.context.ONE_MOD_P == it }
        }
    }

    @Test
    fun closure() {
        runProperty {
            forAll(elementsModQ(), elementsModQ()) { a, b -> (a + b).inBounds() }
            forAll(elementsModQ(), elementsModQ()) { a, b -> (a * b).inBounds() }
            forAll(validElementsModP(), validElementsModP()) { a, b -> (a * b).inBoundsNoZero() }
        }
    }

    @Test
    fun additiveInverse() {
        runProperty {
            forAll(elementsModQ(), elementsModQ()) { a, b -> a - b + b == a }
            forAll(elementsModQ(), elementsModQ()) { a, b ->
                val tmp = -b
                a + tmp + b == a
            }
        }
    }

    @Test
    fun divisionBySelfQ() {
        runProperty { forAll(elementsModQ(minimum = 1)) { it / it == it.context.ONE_MOD_Q } }
    }

    @Test
    fun multInvQ() {
        runProperty {
            forAll(elementsModQ(minimum = 1)) { it * it.multInv() == it.context.ONE_MOD_Q }
        }
    }

    @Test
    fun divisionBySelfP() {
        runProperty { forAll(validElementsModP()) { it / it == it.context.ONE_MOD_P } }
    }

    @Test
    fun multInvP() {
        runProperty { forAll(validElementsModP()) { it * it.multInv() == it.context.ONE_MOD_P } }
    }

    @Test
    fun randRangeQ() {
        val ctx = testGroup()
        runProperty {
            // We have to be careful because an arbitrary integer could be greater than the
            // q test modulus, which would incorrectly crash the test. In practice, the minimum
            // is going to be a small integer, no matter what.

            checkAll(Arb.int(min=0, max=100)) { i ->
                if (i >= 0) {
                    assertTrue(ctx.randRangeQ(minimum = i).inBounds())
                } else {
                    assertFailsWith(IllegalArgumentException::class) {
                        ctx.randRangeQ(minimum = i)
                    }
                }
            }
        }
    }

    // TODO: exponentiation
}