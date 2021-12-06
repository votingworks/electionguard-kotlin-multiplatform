package electionguard

import kotlin.test.Test
import kotlin.test.assertEquals

 class GroupTest {
     @Test
     fun basics() {
         val ctx = lowMemoryProductionGroup()
         val three = 3.toElementModP(ctx)
         val four = 4.toElementModP(ctx)
         val seven = 12.toElementModP(ctx)
         assertEquals(seven, three * four)
     }
 }
//
//     @Test
//     fun comparisonOperations() {
//         val three = 3.toElementModP()
//         val four = 4.toElementModP()
//
//         assertTrue(three < four)
//         assertTrue(three <= four)
//         assertTrue(four > three)
//         assertTrue(four >= four)
//
//         assertTrue(three < 4)
//         assertTrue(four > 2)
//         assertTrue(four > 2L)
//         assertTrue(four > 2.0)
//     }
//
//     @Test
//     fun generatorsWork() {
//         qt().forAll(elementsModP()).check { it.inBounds() }
//
//         qt().forAll(elementsModQ()).check { it.inBounds() }
//     }
//
//     @Test
//     fun validResiduesForGPowP() {
//         qt().forAll(validElementsModP()).check { it.isValidResidue() }
//     }
//
//     @Test
//     fun binaryArrayRoundTrip() {
//         qt().forAll(elementsModP()).check { it == bytesElementModP(it.byteArray()) }
//         qt().forAll(elementsModQ()).check { it == bytesElementModQ(it.byteArray()) }
//     }
//
//     @Test
//     fun base64RoundTrip() {
//         qt().forAll(elementsModP()).check { it == base64ElementModP(it.base64()) }
//         qt().forAll(elementsModQ()).check { it == base64ElementModQ(it.base64()) }
//     }
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