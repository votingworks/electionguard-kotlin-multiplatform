package electionguard.core

import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertEquals

class MontgomeryGroupTest {
    @Test
    fun montgomeryFormMultiplication4096() =
        montgomeryFormMultiplication { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun montgomeryFormMultiplication3072() =
        montgomeryFormMultiplication { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun montgomeryFormMultiplicationTiny() =
        montgomeryFormMultiplication { tinyGroup() }

    fun montgomeryFormMultiplication(contextF: () -> GroupContext) {
        runTest {
            val context = contextF()

            checkAll(
                if (context.isProductionStrength()) propTestFastConfig else propTestSlowConfig,
                validElementsModP(context), validElementsModP(context))
            { a, b ->
                val expected = a * b
                val actual = (a.toMontgomeryElementModP() * b.toMontgomeryElementModP()).toElementModP()
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun montgomeryFormOutAndBack4096() =
        montgomeryFormOutAndBack { productionGroup(mode = ProductionMode.Mode4096) }

    @Test
    fun montgomeryFormOutAndBack3072() =
        montgomeryFormOutAndBack { productionGroup(mode = ProductionMode.Mode3072) }

    @Test
    fun montgomeryFormOutAndBackTiny() =
        montgomeryFormOutAndBack { tinyGroup() }


    fun montgomeryFormOutAndBack(contextF: () -> GroupContext) {
        runTest {
            val context = contextF()

            checkAll(
                if (context.isProductionStrength()) propTestFastConfig else propTestSlowConfig,
                validElementsModP(context)) {
                val outAndBack = it.toMontgomeryElementModP().toElementModP()
                assertEquals(it, outAndBack)
            }
        }
    }
}