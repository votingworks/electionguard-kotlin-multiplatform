package electionguard.core

import io.kotest.property.checkAll
import org.junit.jupiter.api.Test
import java.math.BigInteger
import kotlin.test.assertEquals

// Unlike the "normal" group tests, these ones need to look inside at the internal
// data structures (e.g., BigInteger for Java), so we put these tests in the JVM-only
// section to make this possible.
class MontgomeryJVMTests {
    @Test
    fun shiftyModAndDiv4096() =
        shiftyModAndDiv { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun shiftyModAndDiv3072() =
        shiftyModAndDiv { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun shiftyModAndDivTiny() = runTest {
        val context = tinyGroup()

        checkAll(validElementsModP(context), validElementsModP(context)) { a, b ->
            val aVal = a.toMontgomeryElementModP() as TinyMontgomeryElementModP
            val bVal = b.toMontgomeryElementModP() as TinyMontgomeryElementModP
            with (aVal) {
                val twoPowPBits: ULong = 1UL shl context.NUM_P_BITS
                assertEquals(element.toULong(), element.toULong().modI())
                assertEquals(element.toULong(), (element.toULong() + (bVal.element.toULong() * twoPowPBits)).modI())
                assertEquals(element, (element.toULong() * twoPowPBits).divI())
            }
        }
    }

    fun shiftyModAndDiv(contextF: () -> GroupContext) {
        runTest {
            val context = contextF() as ProductionGroupContext

            checkAll(
                propTestSlowConfig,
                validElementsModP(context), validElementsModP((context))
            ) { a, b ->
                val aVal = a.toMontgomeryElementModP() as ProductionMontgomeryElementModP
                val bVal = b.toMontgomeryElementModP() as ProductionMontgomeryElementModP
                with (aVal) {
                    val twoPowPBits = BigInteger.TWO.pow(context.NUM_P_BITS)
                    assertEquals(element, element.modI())
                    assertEquals(element, (element + (bVal.element * twoPowPBits)).modI())
                    assertEquals(element, (element * twoPowPBits).divI())
                    assertEquals(element, (element * twoPowPBits + bVal.element).divI())
                }
            }
        }
    }

    @Test
    fun relationshipsIAndPTiny() {
        runTest {
            val pPrime = intTestMontgomeryPPrime.toULong()
            val p = intTestP.toULong()
            val iPrime = intTestMontgomeryIPrime.toULong()
            val i = intTestMontgomeryI.toULong()

            assertEquals(1UL, (iPrime * i) % p)
            assertEquals(1UL, ((i - pPrime) * p) % i)
        }
    }
    @Test
    fun relationshipsIAndPProduction() {
        runTest {
            listOf(productionGroup(mode = ProductionMode.Mode4096), productionGroup(mode = ProductionMode.Mode3072))
                .forEach { context ->
                    val pContext = context as ProductionGroupContext
                    val pPrime = pContext.montgomeryPPrime
                    val p = pContext.p
                    val iPrime = pContext.montgomeryIPrime
                    val i = pContext.montgomeryIMinusOne + BigInteger.ONE

                    assertEquals(BigInteger.ONE, (iPrime * i) % p)
                    assertEquals(BigInteger.ONE, ((i - pPrime) * p) % i)
                }
        }
    }
}