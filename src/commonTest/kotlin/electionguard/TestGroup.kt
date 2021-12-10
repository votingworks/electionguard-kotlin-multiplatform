package electionguard

import io.kotest.property.forAll
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun validResiduesForGPowP() {
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
}
//
//     @Test
//     fun base16RoundTrip() {
//         qt().forAll(elementsModP()).check { it == base16ElementModP(it.base16()) }
//         qt().forAll(elementsModQ()).check { it == base16ElementModQ(it.base16()) }
//     }
//
//     @Test
//     fun base10RoundTrip() {
//         qt().forAll(elementsModP()).check { it == base10ElementModP(it.base10()) }
//         qt().forAll(elementsModQ()).check { it == base10ElementModQ(it.base10()) }
//     }
//
//     @TestFactory
//     fun baseConversionFails(): Iterable<DynamicTest> {
//         logger.warn {
//             "About to test for errors: logs will have many errors, but the tests should pass"
//         }
//         return listOf("", "@@", "-10", "1234567890".repeat(1000))
//             .flatMap {
//                 listOf(
//                     dynamicTest("base64ElementModP($it)") { assertNull(base64ElementModP(it)) },
//                     dynamicTest("base64ElementModQ($it)") { assertNull(base64ElementModQ(it)) },
//                     dynamicTest("base16ElementModP($it)") { assertNull(base16ElementModP(it)) },
//                     dynamicTest("base16ElementModQ($it)") { assertNull(base16ElementModQ(it)) },
//                     dynamicTest("base10ElementModP($it)") { assertNull(base10ElementModP(it)) },
//                     dynamicTest("base10ElementModQ($it)") { assertNull(base10ElementModQ(it)) },
//                 )
//             }
//     }
//
//     @Test
//     fun additionBasics() {
//         qt().forAll(elementsModP("a"), elementsModP("b"))
//             .checkAssert { a, b ->
//                 val expected = a.element + b.element
//                 val actual = a + b
//                 assertEquals(expected, actual.element)
//                 assertEquals("[\"addP\",\"a\",\"b\"]", actual.formula.toString())
//             }
//         qt().forAll(elementsModQ("a"), elementsModQ("b"))
//             .checkAssert { a, b ->
//                 val expected = a.element + b.element
//                 val actual = a + b
//                 val actual2 = addQ(a, b)
//                 assertEquals(expected, actual.element)
//                 assertEquals(expected, actual2.element)
//                 assertEquals("[\"addQ\",\"a\",\"b\"]", actual.formula.toString())
//                 assertEquals("[\"addQ\",\"a\",\"b\"]", actual2.formula.toString())
//             }
//     }
//
//     @Test
//     fun multiplicationBasicsP() {
//         qt().forAll(elementsModP("a"), elementsModP("b"))
//             .checkAssert { a, b ->
//                 val expected = a.element * b.element
//                 val actual = a * b
//                 val actual2 = multP(a, b)
//                 assertEquals(expected, actual.element)
//                 assertEquals(expected, actual2.element)
//                 assertEquals("[\"multP\",\"a\",\"b\"]", actual.formula.toString())
//                 assertEquals("[\"multP\",\"a\",\"b\"]", actual2.formula.toString())
//             }
//     }
//
//     @Test
//     fun multiplicationBasicsQ() {
//         qt().forAll(elementsModQ("a"), elementsModQ("b"))
//             .checkAssert { a, b ->
//                 val expected = a.element * b.element
//                 val actual = a * b
//                 assertEquals(expected, actual.element)
//                 assertEquals("[\"multQ\",\"a\",\"b\"]", actual.formula.toString())
//             }
//     }
//
//     @Test
//     fun subtractionBasics() {
//         qt().forAll(elementsModP("a"), elementsModP("b"))
//             .checkAssert { a, b ->
//                 val expected = a.element - b.element
//                 val actual = a - b
//                 assertEquals(expected, actual.element)
//                 assertEquals("[\"minusP\",\"a\",\"b\"]", actual.formula.toString())
//             }
//         qt().forAll(elementsModQ("a"), elementsModQ("b"))
//             .checkAssert { a, b ->
//                 val expected = a.element - b.element
//                 val actual = a - b
//                 assertEquals(expected, actual.element)
//                 assertEquals("[\"minusQ\",\"a\",\"b\"]", actual.formula.toString())
//             }
//     }
//
//     @Test
//     fun negation() {
//         qt().forAll(elementsModQ("a"))
//             .checkAssert {
//                 assertEquals(ZERO_MOD_Q, negateQ(it) + it)
//                 assertEquals("[\"negQ\",\"a\"]", negateQ(it).formula.toString())
//             }
//     }
//
//     @Test
//     fun randRangeQ() {
//         qt().forAll(integers().all())
//             .checkAssert {
//                 if (it >= 0) {
//                     assertTrue(randRangeQ(it).inBounds())
//                 } else {
//                     assertThrows<GroupException> { randRangeQ(it) }
//                 }
//             }
//     }
//
//     @Test
//     fun multiplicativeInversesP() {
//         qt().forAll(elementsModPNoZero()).check { it.multInv() * it == ONE_MOD_P }
//     }
//
//     @Test
//     fun multiplicativeInversesQ() {
//         qt().forAll(elementsModQNoZero()).check { it.multInv() * it == ONE_MOD_Q }
//     }
//
//     @Test
//     fun divisionP() {
//         qt().forAll(elementsModPNoZero()).check { it divP it == ONE_MOD_P }
//     }
//
//     @Test
//     fun divisionQ() {
//         qt().forAll(elementsModQNoZero()).check { it divQ it == ONE_MOD_Q }
//     }
//
//     @Test
//     fun moreComplexFormulas() {
//         assertEquals("\"0\"", ZERO_MOD_Q.formula.toString())
//         assertEquals("\"0\"", ZERO_MOD_P.formula.toString())
//         assertEquals("\"1\"", ONE_MOD_Q.formula.toString())
//         assertEquals("\"1\"", ONE_MOD_P.formula.toString())
//         assertEquals("\"2\"", TWO_MOD_Q.formula.toString())
//         assertEquals("\"2\"", TWO_MOD_P.formula.toString())
//
//         val a = 0.toElementModP("a")
//         val b = 0.toElementModQ("b")
//         val c = 0.toElementModQ("c")
//         val r = a * gPowP(b + c)
//
//         // we've got three different ways to convert these things to strings
//
//         assertEquals("""["multP","a",["gPowP",["addQ","b","c"]]]""", r.formula.toString())
//         assertEquals(
//             """ElementModP(formula = ["multP","a",["gPowP",["addQ","b","c"]]], element = 0)""",
//             r.toString()
//         )
//
//         // relatively short equations become one-liners
//         assertEquals("""ElementModP["multP","a",["gPowP",["addQ","b","c"]]]""",
// r.toFormulaString())
//
//         val bigger = hashElements(r, r, r, r, r, r, r)
//         val expected =
//             """ElementModQ[
//     "hash",
//     ["multP","a",["gPowP",["addQ","b","c"]]],
//     ["multP","a",["gPowP",["addQ","b","c"]]],
//     ["multP","a",["gPowP",["addQ","b","c"]]],
//     ["multP","a",["gPowP",["addQ","b","c"]]],
//     ["multP","a",["gPowP",["addQ","b","c"]]],
//     ["multP","a",["gPowP",["addQ","b","c"]]],
//     ["multP","a",["gPowP",["addQ","b","c"]]]
// ]"""
//         assertEquals(expected, bigger.toFormulaString())
//     }
// }
//